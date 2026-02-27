package com.slg.common.executor;

/**
 * 虚拟线程任务标识
 * 结构化 key，由模块枚举和 long 类型的 ID 组成，用于 {@link KeyedVirtualExecutor} 按 key 有序执行
 *
 * <p>使用方式：
 * <ul>
 *   <li>多链（模块 + ID）：{@code TaskKey.of(TaskModule.PLAYER, playerId)} — 同模块下按 ID 分链</li>
 *   <li>单链（仅模块）：{@code TaskKey.of(TaskModule.SYSTEM)} — 该模块所有任务共用一条串行链</li>
 * </ul>
 *
 * <p>日志展示：{@code toString()} 返回 {@code "[player:12345]"} 或 {@code "[system]"}，
 * 消费者虚拟线程名设为 {@code "vt-[player:12345]"}，便于日志排查。
 *
 * <p>id 使用原始类型 long，避免 Long 自动装箱的性能损耗；
 * 单链模式下 id 为 0（约定业务 ID 不会使用 0）。
 *
 * @author yangxunan
 * @date 2026/02/07
 */
public record TaskKey(TaskModule module, long id) {

    /**
     * 单链模式的 id 值（无 ID）
     */
    private static final long SINGLE_CHAIN_ID = 0L;

    /**
     * 创建多链 TaskKey（模块 + ID）
     * 同模块下相同 ID 的任务串行执行，不同 ID 的任务并发执行
     *
     * @param module 模块枚举
     * @param id     标识（如 playerId、entityId），不能为 0
     * @return TaskKey 实例
     */
    public static TaskKey of(TaskModule module, long id) {
        return new TaskKey(module, id);
    }

    /**
     * 创建单链 TaskKey（仅模块）
     * 该模块所有任务共用一条串行链
     *
     * @param module 模块枚举
     * @return TaskKey 实例
     */
    public static TaskKey of(TaskModule module) {
        return new TaskKey(module, SINGLE_CHAIN_ID);
    }

    /**
     * 是否为单链模式（无 ID）
     *
     * @return true 表示单链模式
     */
    public boolean isSingleChain() {
        return id == SINGLE_CHAIN_ID;
    }

    /**
     * 返回日志友好的字符串表示
     * 多链模式返回 {@code "[player:12345]"}，单链模式返回 {@code "[system]"}
     *
     * @return 格式化字符串
     */
    @Override
    public String toString() {
        if (id != SINGLE_CHAIN_ID) {
            return "[" + module + ":" + id + "]";
        }
        return "[" + module + "]";
    }
}
