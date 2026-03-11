package com.slg.common.executor;

import com.slg.common.executor.core.MultiExecutor;
import com.slg.common.executor.core.SingleExecutor;
import com.slg.common.log.LoggerUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 统一执行器管理类
 * 持有各模块的执行器静态引用，替代各业务模块独立的线程池管理
 *
 * <p>多链模块使用 {@link MultiExecutor}（按 key 分链），单链模块使用 {@link SingleExecutor}（共用一条链），
 * 通过类型系统在编译期防止调用错误（如对单链模块误传 key 或对多链模块漏传 key）。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 玩家任务（多链，按 playerId 分链）
 * Executor.Player.execute(playerId, task);
 *
 * // 系统任务（单链）
 * Executor.System.execute(task);
 *
 * // 登录任务（单链）
 * Executor.Login.execute(task);
 *
 * // 场景任务（单链）
 * Executor.Scene.execute(task);
 *
 * // 持久化任务（多链，按实体 ID 分链）
 * Executor.Persistence.execute(entityId, task);
 *
 * // 机器人任务（多链，按 robotId 分链）
 * Executor.Robot.execute(robotId, task);
 * }</pre>
 *
 * @author yangxunan
 * @date 2026/02/09
 */
@Component
public class Executor {

    /**
     * 玩家模块执行器（多链，按 playerId 分链）
     */
    public static MultiExecutor Player;

    /**
     * 场景模块 按nodeId分配业务
     */
    public static MultiExecutor SceneNode;

    /**
     * 系统模块执行器（单链）
     */
    public static SingleExecutor System;

    /**
     * 登录模块执行器（单链）
     */
    public static SingleExecutor Login;

    /**
     * 场景模块执行器（单链）
     */
    public static SingleExecutor Scene;

    /**
     * 持久化模块执行器（多链，按实体 ID 分链）
     */
    public static MultiExecutor Persistence;

    /**
     * 机器人模块执行器（多链，按 robotId 分链）
     */
    public static MultiExecutor Robot;

    /**
     * RPC 响应模块执行器（单链）
     * 所有 RPC 响应回调和超时处理在此链中串行执行
     */
    public static SingleExecutor RpcResponse;


    /**
     * 初始化所有模块执行器
     * 根据 {@link TaskModule} 枚举定义自动创建 {@link MultiExecutor} 或 {@link SingleExecutor}，
     * 并通过反射赋值给对应的静态字段。
     *
     * <p>字段名通过 {@link TaskModule#getName()} 首字母大写推导。
     * 若 TaskModule 没有对应字段、字段类型与模块链类型不匹配、或存在未关联 TaskModule 的字段，将抛出异常。
     */
    @PostConstruct
    void init() {
        Map<String, Field> executorFields = new HashMap<>();
        for (Field field : Executor.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    && (field.getType() == MultiExecutor.class || field.getType() == SingleExecutor.class)) {
                executorFields.put(field.getName(), field);
            }
        }

        Set<String> assignedFields = new HashSet<>();

        for (TaskModule module : TaskModule.values()) {
            String fieldName = capitalize(module.getName());

            Field field = executorFields.get(fieldName);
            if (field == null) {
                throw new IllegalStateException(
                        "TaskModule." + module.name() + " 没有对应的 Executor 静态字段: " + fieldName);
            }

            if (module.isMultiChain()) {
                if (field.getType() != MultiExecutor.class) {
                    throw new IllegalStateException(
                            "TaskModule." + module.name() + " 为多链模块，但字段 " + fieldName
                                    + " 类型为 " + field.getType().getSimpleName() + "，应为 MultiExecutor");
                }
                setStaticField(field, new MultiExecutor(module));
            } else {
                if (field.getType() != SingleExecutor.class) {
                    throw new IllegalStateException(
                            "TaskModule." + module.name() + " 为单链模块，但字段 " + fieldName
                                    + " 类型为 " + field.getType().getSimpleName() + "，应为 SingleExecutor");
                }
                setStaticField(field, new SingleExecutor(module));
            }

            assignedFields.add(fieldName);
            LoggerUtil.debug("Executor.{} 已初始化 → {}（模块: {}）",
                    fieldName, module.isMultiChain() ? "MultiExecutor" : "SingleExecutor", module.name());
        }

        for (String fieldName : executorFields.keySet()) {
            if (!assignedFields.contains(fieldName)) {
                throw new IllegalStateException(
                        "Executor 字段 " + fieldName + " 没有对应的 TaskModule 定义，请在 TaskModule 中补充");
            }
        }

        LoggerUtil.debug("所有 Executor 模块初始化完成，共 {} 个", assignedFields.size());
    }

    /**
     * 将字符串首字母大写
     *
     * @param str 原始字符串
     * @return 首字母大写后的字符串
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 通过反射设置静态字段的值
     *
     * @param field 要设置的字段
     * @param value 要赋的值
     */
    private static void setStaticField(Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(null, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("无法设置 Executor 静态字段: " + field.getName(), e);
        }
    }
}
