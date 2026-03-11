package com.slg.common.executor.core;

import com.slg.common.executor.TaskModule;

import java.util.Map;

/**
 * 执行器运行时指标快照
 * 由 {@link KeyedVirtualExecutor#getMetrics()} 生成，用于监控和运维
 *
 * @param modules 各模块的指标
 * @author yangxunan
 * @date 2026/03/11
 */
public record ExecutorMetrics(Map<TaskModule, ModuleMetrics> modules) {

    /**
     * 单个模块的指标
     *
     * @param multiChain      是否为多链模块
     * @param totalQueueSize  所有链队列中的待处理任务总数
     * @param totalRejected   累计被拒绝的任务总数
     * @param activeConsumers 当前活跃的消费者线程数
     * @param chainCount      链数量（多链为活跃 key 数，单链为 1）
     */
    public record ModuleMetrics(
            boolean multiChain,
            int totalQueueSize,
            long totalRejected,
            int activeConsumers,
            int chainCount
    ) {}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ExecutorMetrics{\n");
        for (var entry : modules.entrySet()) {
            TaskModule module = entry.getKey();
            ModuleMetrics m = entry.getValue();
            sb.append(String.format("  %s: queued=%d, rejected=%d, active=%d, chains=%d%n",
                    module, m.totalQueueSize, m.totalRejected, m.activeConsumers, m.chainCount));
        }
        sb.append("}");
        return sb.toString();
    }
}
