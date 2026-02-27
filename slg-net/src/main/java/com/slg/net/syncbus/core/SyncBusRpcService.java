package com.slg.net.syncbus.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.slg.common.log.LoggerUtil;
import com.slg.common.util.JsonUtil;
import com.slg.net.rpc.impl.syncbus.ISyncBusRpcService;
import com.slg.net.syncbus.ISyncCache;
import com.slg.net.syncbus.ISyncCacheResolver;
import com.slg.net.syncbus.SyncModule;
import com.slg.net.syncbus.codec.ISyncFieldDecoder;
import com.slg.net.syncbus.model.SyncCacheFieldMeta;
import com.slg.net.syncbus.model.SyncCacheMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 同步总线 RPC 接收端实现（Cache 端使用）
 * 接收来自 Holder 端的同步数据，反序列化后写入 Cache 实体
 * <p>
 * RPC 框架根据 useModule = TaskModule.PLAYER + @ThreadKey entityId
 * 自动分派到 PLAYER 多链执行器中，保证同一实体串行执行
 *
 * @author yangxunan
 * @date 2026/02/12
 */
@Component
public class SyncBusRpcService implements ISyncBusRpcService {

    /** Map&lt;String, String&gt; 的 TypeReference，用于 JSON 反序列化 */
    private static final TypeReference<Map<String, String>> FIELD_DATA_TYPE =
            new TypeReference<>() {};

    @Autowired
    private SyncBusRegistry registry;

    @Override
    public void receiveSyncData(int targetServerId, long entityId, int syncModuleId, String fieldData) {
        // 还原 SyncModule 枚举
        SyncModule module = SyncModule.fromId(syncModuleId);
        if (module == null) {
            LoggerUtil.error("[SyncBus] 未知的 syncModuleId: {}", syncModuleId);
            return;
        }

        // 获取 Cache 端元数据
        SyncCacheMeta cacheMeta = registry.getCacheMeta(module);
        if (cacheMeta == null) {
            LoggerUtil.error("[SyncBus] 未找到 SyncModule {} 的 Cache 元数据", module);
            return;
        }

        // 通过 Resolver 查找 Cache 实体
        ISyncCacheResolver<?> resolver = cacheMeta.getResolver();
        if (resolver == null) {
            LoggerUtil.error("[SyncBus] SyncModule {} 没有对应的 ISyncCacheResolver", module);
            return;
        }

        ISyncCache cache = resolver.resolve(entityId);
        if (cache == null) {
            // Cache 实体不存在，对端可能尚未创建，属正常情况
            LoggerUtil.debug("[SyncBus] Cache 实体不存在，可能尚未创建: module={}, entityId={}", module, entityId);
            return;
        }

        // 反序列化 fieldData
        Map<String, String> fields;
        try {
            fields = JsonUtil.fromJson(fieldData, FIELD_DATA_TYPE);
        } catch (Exception e) {
            LoggerUtil.error("[SyncBus] fieldData 反序列化失败: module={}, entityId={}", module, entityId, e);
            return;
        }

        if (fields == null || fields.isEmpty()) {
            return;
        }

        // 逐字段处理
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            String encodedValue = entry.getValue();

            SyncCacheFieldMeta fieldMeta = cacheMeta.getFields().get(fieldName);
            if (fieldMeta == null) {
                // 字段不匹配（Cache 端没有该字段），打印 error 日志
                LoggerUtil.error("[SyncBus] Cache 端字段不匹配: module={}, entityId={}, fieldName={}",
                        module, entityId, fieldName);
                continue;
            }

            try {
                // 解码
                Object decodedValue = decodeValue(encodedValue, fieldMeta);

                // 通过 MethodHandle 写入字段值
                fieldMeta.getSetter().invoke(cache, decodedValue);

                // 触发回调
                cache.onSyncUpdated(fieldName, decodedValue);

            } catch (Throwable e) {
                LoggerUtil.error("[SyncBus] 字段同步失败: module={}, entityId={}, fieldName={}",
                        module, entityId, fieldName, e);
            }
        }
    }

    /**
     * 解码字段值
     * 有自定义 Decoder 则使用 Decoder，否则使用 JsonUtil.fromJson
     *
     * @param encodedValue 编码后的字符串
     * @param fieldMeta    字段元数据
     * @return 解码后的值
     */
    @SuppressWarnings("unchecked")
    private Object decodeValue(String encodedValue, SyncCacheFieldMeta fieldMeta) {
        ISyncFieldDecoder<?> decoder = fieldMeta.getDecoder();
        if (decoder != null) {
            return ((ISyncFieldDecoder<Object>) decoder).decode(encodedValue);
        }
        return JsonUtil.fromJson(encodedValue, fieldMeta.getFieldType());
    }
}
