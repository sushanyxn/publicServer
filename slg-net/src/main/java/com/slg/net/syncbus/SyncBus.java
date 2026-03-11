package com.slg.net.syncbus;

import com.slg.common.executor.core.GlobalScheduler;
import com.slg.common.executor.core.KeyedVirtualExecutor;
import com.slg.common.executor.core.TaskKey;
import com.slg.common.log.LoggerUtil;
import com.slg.common.util.JsonUtil;
import com.slg.net.rpc.anno.RpcRef;
import com.slg.net.rpc.impl.syncbus.ISyncBusRpcService;
import com.slg.net.syncbus.anno.SyncEntity;
import com.slg.net.syncbus.codec.ISyncFieldEncoder;
import com.slg.net.syncbus.core.SyncBusRegistry;
import com.slg.net.syncbus.model.SyncHolderFieldMeta;
import com.slg.net.syncbus.model.SyncHolderMeta;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 同步总线入口（Holder 端使用）
 * 提供 sync / syncAll / remove 静态方法，供业务代码一行调用完成跨进程同步
 * <p>
 * 内部集成限流机制，按字段级别控制发送频率，避免高频变更产生大量 RPC 调用
 *
 * @author yangxunan
 * @date 2026/02/12
 */
@Component
public class SyncBus {

    @Autowired
    private SyncBusRegistry registry;

    @RpcRef
    private ISyncBusRpcService rpcService;

    /** 静态实例，供静态方法使用 */
    private static SyncBus instance;

    /**
     * 限流状态表：以实体 syncId 为外层 key
     * 外层 ConcurrentHashMap：不同实体可能从不同执行器链访问
     * 内层 HashMap：同一实体的所有 sync() 调用在同一执行器链内串行执行
     */
    private static final ConcurrentHashMap<Long, Map<String, SyncThrottleState>> throttleStates =
            new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        instance = this;
        LoggerUtil.info("[SyncBus] 同步总线初始化完成");
    }

    /**
     * 同步指定字段到远端（受限流控制）
     *
     * @param holder     Holder 端实体
     * @param fieldNames 要同步的字段名列表
     */
    public static void sync(ISyncHolder holder, String... fieldNames) {
        if (instance == null) {
            LoggerUtil.error("[SyncBus] SyncBus 尚未初始化");
            return;
        }
        if (fieldNames == null || fieldNames.length == 0) {
            return;
        }

        SyncModule module = getSyncModule(holder.getClass());
        if (module == null) {
            LoggerUtil.error("[SyncBus] 类 {} 未标注 @SyncEntity 注解", holder.getClass().getName());
            return;
        }

        SyncHolderMeta holderMeta = instance.registry.getHolderMeta(module);
        if (holderMeta == null) {
            LoggerUtil.error("[SyncBus] 未找到 SyncModule {} 的 Holder 元数据", module);
            return;
        }

        // 捕获当前线程的 TaskKey（用于限流延迟任务调度）
        TaskKey taskKey = KeyedVirtualExecutor.currentTaskKey();

        for (String fieldName : fieldNames) {
            SyncHolderFieldMeta fieldMeta = holderMeta.getFields().get(fieldName);
            if (fieldMeta == null) {
                LoggerUtil.error("[SyncBus] 字段 {} 未在 {} 中标注 @SyncField",
                        fieldName, holder.getClass().getSimpleName());
                continue;
            }

            if (fieldMeta.getSyncIntervalMs() == 0) {
                // 不限流，直接发送
                doSendField(holder, module, fieldMeta);
            } else {
                // 走限流判断
                handleThrottle(holder, module, fieldMeta, taskKey);
            }
        }
    }

    /**
     * 同步所有 @SyncField 字段到远端（不经过限流，立即全量发送）
     * 与 sync() 的限流机制完全独立，不重置限流状态
     *
     * @param holder Holder 端实体
     */
    public static void syncAll(ISyncHolder holder) {
        if (instance == null) {
            LoggerUtil.error("[SyncBus] SyncBus 尚未初始化");
            return;
        }

        SyncModule module = getSyncModule(holder.getClass());
        if (module == null) {
            LoggerUtil.error("[SyncBus] 类 {} 未标注 @SyncEntity 注解", holder.getClass().getName());
            return;
        }

        SyncHolderMeta holderMeta = instance.registry.getHolderMeta(module);
        if (holderMeta == null) {
            LoggerUtil.error("[SyncBus] 未找到 SyncModule {} 的 Holder 元数据", module);
            return;
        }

        // 立即读取所有字段并发送
        Map<String, String> fieldData = new HashMap<>();
        for (SyncHolderFieldMeta fieldMeta : holderMeta.getFields().values()) {
            try {
                String encodedValue = readAndEncode(holder, fieldMeta);
                fieldData.put(fieldMeta.getFieldName(), encodedValue);
            } catch (Throwable e) {
                LoggerUtil.error("[SyncBus] syncAll 读取字段 {} 失败",
                        fieldMeta.getFieldName(), e);
            }
        }

        if (fieldData.isEmpty()) {
            return;
        }

        // 发送到所有目标服务器
        sendToTargets(holder, module, fieldData);
    }

    /**
     * 移除指定实体的所有限流状态
     * 限流状态在定时发送后会自动清理，此方法作为安全兜底
     * 业务方可在实体生命周期结束时调用（如玩家下线、实体销毁）
     * <p>
     * 线程安全说明：此方法必须在对应实体的执行器链内调用（与 sync() 在同一条串行链），
     * 以避免与定时器回调中的 removeThrottleState 产生竞态。
     * 通常在玩家下线流程中调用，自然满足该约束。
     *
     * @param syncId 实体唯一标识（即 ISyncHolder.getSyncId()）
     */
    public static void remove(long syncId) {
        throttleStates.remove(syncId);
    }

    // ======================== 限流机制 ========================

    /**
     * 处理限流判断
     * <p>
     * 限流状态自维护（定时器驱动自清理）：
     * <ul>
     *   <li>无状态 → 立即发送，创建状态并启动定时器</li>
     *   <li>有状态 → 标记 dirty，等待定时器处理</li>
     *   <li>定时器到期且 dirty → 发送最新值，重置 dirty，再次定时</li>
     *   <li>定时器到期且非 dirty → 无新更新，移除状态自清理</li>
     * </ul>
     *
     * @param holder    Holder 实体
     * @param module    同步模块
     * @param fieldMeta 字段元数据
     * @param taskKey   当前线程的 TaskKey（用于延迟任务调度）
     */
    private static void handleThrottle(ISyncHolder holder, SyncModule module,
                                       SyncHolderFieldMeta fieldMeta, TaskKey taskKey) {
        long syncId = holder.getSyncId();
        String fieldName = fieldMeta.getFieldName();
        long syncIntervalMs = fieldMeta.getSyncIntervalMs();

        SyncThrottleState state = getThrottleState(syncId, fieldName);

        if (state != null) {
            // 有限流状态，标记有新更新，等定时器处理
            state.dirty = true;
            return;
        }

        // 无限流状态，立即发送
        doSendField(holder, module, fieldMeta);

        // 创建状态并启动定时器（非虚拟线程环境不启动定时器，无法自清理则不创建状态）
        if (taskKey != null) {
            GlobalScheduler globalScheduler = GlobalScheduler.getInstance();
            if (globalScheduler != null) {
                SyncThrottleState newState = new SyncThrottleState();
                putThrottleState(syncId, fieldName, newState);
                scheduleThrottleCheck(holder, module, fieldMeta, syncId, fieldName,
                        newState, taskKey, syncIntervalMs);
            }
        }
    }

    /**
     * 调度限流定时检查
     * 定时器到期后检查 dirty 标记：有更新则发送并重新定时，无更新则移除状态自清理
     *
     * @param holder        Holder 实体
     * @param module        同步模块
     * @param fieldMeta     字段元数据
     * @param syncId        实体同步 ID
     * @param fieldName     字段名
     * @param state         限流状态（定时器与 sync 共享同一对象引用）
     * @param taskKey       任务标识（保证回到原执行器链串行执行）
     * @param syncIntervalMs 限流间隔（毫秒）
     */
    private static void scheduleThrottleCheck(ISyncHolder holder, SyncModule module,
                                              SyncHolderFieldMeta fieldMeta,
                                              long syncId, String fieldName,
                                              SyncThrottleState state,
                                              TaskKey taskKey, long syncIntervalMs) {
        GlobalScheduler globalScheduler = GlobalScheduler.getInstance();
        if (globalScheduler == null) {
            return;
        }
        globalScheduler.schedule(taskKey, () -> {
            if (state.dirty) {
                // 有新更新，发送最新值并重新定时
                state.dirty = false;
                doSendField(holder, module, fieldMeta);
                scheduleThrottleCheck(holder, module, fieldMeta, syncId, fieldName,
                        state, taskKey, syncIntervalMs);
            } else {
                // 无新更新，移除状态自清理
                removeThrottleState(syncId, fieldName);
            }
        }, syncIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 尝试获取限流状态（不创建）
     *
     * @param syncId    实体同步 ID
     * @param fieldName 字段名
     * @return 限流状态，不存在时返回 null
     */
    private static SyncThrottleState getThrottleState(long syncId, String fieldName) {
        Map<String, SyncThrottleState> entityStates = throttleStates.get(syncId);
        if (entityStates == null) {
            return null;
        }
        return entityStates.get(fieldName);
    }

    /**
     * 放入限流状态
     *
     * @param syncId    实体同步 ID
     * @param fieldName 字段名
     * @param state     限流状态对象
     */
    private static void putThrottleState(long syncId, String fieldName, SyncThrottleState state) {
        throttleStates.computeIfAbsent(syncId, k -> new HashMap<>()).put(fieldName, state);
    }

    /**
     * 移除指定字段的限流状态
     * 若该 syncId 下已无任何字段的限流状态，则同时移除整个 syncId 条目
     *
     * @param syncId    实体同步 ID
     * @param fieldName 字段名
     */
    private static void removeThrottleState(long syncId, String fieldName) {
        Map<String, SyncThrottleState> entityStates = throttleStates.get(syncId);
        if (entityStates != null) {
            entityStates.remove(fieldName);
            if (entityStates.isEmpty()) {
                throttleStates.remove(syncId);
            }
        }
    }

    // ======================== 数据发送 ========================

    /**
     * 读取单个字段并发送
     *
     * @param holder    Holder 实体
     * @param module    同步模块
     * @param fieldMeta 字段元数据
     */
    private static void doSendField(ISyncHolder holder, SyncModule module, SyncHolderFieldMeta fieldMeta) {
        try {
            String encodedValue = readAndEncode(holder, fieldMeta);
            Map<String, String> fieldData = new HashMap<>(2);
            fieldData.put(fieldMeta.getFieldName(), encodedValue);
            sendToTargets(holder, module, fieldData);
        } catch (Throwable e) {
            LoggerUtil.error("[SyncBus] 发送字段 {} 失败", fieldMeta.getFieldName(), e);
        }
    }

    /**
     * 通过 MethodHandle 读取字段值并编码为 String
     *
     * @param holder    Holder 实体
     * @param fieldMeta 字段元数据
     * @return 编码后的字符串
     * @throws Throwable MethodHandle 调用异常
     */
    @SuppressWarnings("unchecked")
    private static String readAndEncode(ISyncHolder holder, SyncHolderFieldMeta fieldMeta) throws Throwable {
        Object value = fieldMeta.getGetter().invoke(holder);
        if (fieldMeta.getEncoder() != null) {
            return ((ISyncFieldEncoder<Object>) fieldMeta.getEncoder()).encode(value);
        }
        return JsonUtil.toJson(value);
    }

    /**
     * 将 fieldData 发送到所有目标服务器
     *
     * @param holder    Holder 实体
     * @param module    同步模块
     * @param fieldData 字段名 -> 编码后的值
     */
    private static void sendToTargets(ISyncHolder holder, SyncModule module, Map<String, String> fieldData) {
        String json = JsonUtil.toJson(fieldData);
        int[] targetServerIds = holder.syncTargetServerIds();
        if (targetServerIds == null || targetServerIds.length == 0) {
            return;
        }

        long entityId = holder.getSyncId();
        int moduleId = module.getId();

        for (int serverId : targetServerIds) {
            try {
                instance.rpcService.receiveSyncData(serverId, entityId, moduleId, json);
            } catch (Exception e) {
                LoggerUtil.error("[SyncBus] RPC 发送失败: serverId={}, entityId={}, module={}",
                        serverId, entityId, module, e);
            }
        }
    }

    // ======================== 工具方法 ========================

    /**
     * 从类的 @SyncEntity 注解获取 SyncModule
     *
     * @param clazz 实体类
     * @return SyncModule，未找到注解时返回 null
     */
    private static SyncModule getSyncModule(Class<?> clazz) {
        // 向上查找类层级（支持继承）
        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            SyncEntity syncEntity = current.getAnnotation(SyncEntity.class);
            if (syncEntity != null) {
                return syncEntity.value();
            }
        }
        return null;
    }

    // ======================== 限流状态内部类 ========================

    /**
     * 每个实体每个字段的限流状态
     * 同一实体的所有操作在同一执行器链内串行，无需加锁
     * <p>
     * 状态存在即表示"正在限流周期中"，定时器到期后若无新更新则自动移除
     */
    private static class SyncThrottleState {
        /** 是否有新的更新等待发送（定时器到期时检查） */
        boolean dirty;
    }
}
