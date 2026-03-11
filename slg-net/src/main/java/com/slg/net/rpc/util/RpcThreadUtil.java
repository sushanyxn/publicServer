package com.slg.net.rpc.util;

import com.slg.common.executor.core.KeyedVirtualExecutor;
import com.slg.common.executor.TaskModule;
import com.slg.common.log.LoggerUtil;
import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.rpc.manager.RpcProxyManager;
import com.slg.net.rpc.model.RpcMethodMeta;

/**
 * RPC 线程静态工具类
 * 提供 RPC 请求的线程分派方法，直接使用 {@link KeyedVirtualExecutor} 执行
 *
 * @author yangxunan
 * @date 2026/01/23
 */
public class RpcThreadUtil {

    /**
     * 私有构造函数，防止实例化
     */
    private RpcThreadUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 分派 RPC 请求到对应的虚拟线程链中执行
     * 根据 RpcMethodMeta 中的 TaskModule 和 ThreadKey 进行分派
     *
     * @param request RPC 请求
     * @param task    要执行的任务
     */
    public static void dispatch(IM_RpcRequest request, Runnable task) {
        RpcMethodMeta meta = RpcProxyManager.getInstance().getRpcMethodMeta(request.getMethodMarker());
        if (meta == null) {
            LoggerUtil.error("[RPC] 未找到方法元数据: {}", request.getMethodMarker());
            return;
        }

        TaskModule module = meta.getTaskModule();
        KeyedVirtualExecutor executor = KeyedVirtualExecutor.getInstance();
        if (module.isMultiChain()) {
            long key = extractThreadKey(meta, request.getParams());
            executor.execute(module, key, task);
        } else {
            executor.execute(module, task);
        }
    }

    /**
     * 提取 ThreadKey 参数值
     * 支持从方法元数据和参数中提取 ThreadKey
     *
     * @param meta   方法元数据
     * @param params 方法参数
     * @return ThreadKey 值，如果未设置或提取失败则返回 0
     */
    public static long extractThreadKey(RpcMethodMeta meta, Object[] params) {
        if (meta == null || meta.getThreadKeyIndex() < 0 || params == null ||
                meta.getThreadKeyIndex() >= params.length) {
            return 0;
        }

        Object keyParam = params[meta.getThreadKeyIndex()];
        if (keyParam instanceof Long) {
            return (Long) keyParam;
        } else if (keyParam instanceof Integer) {
            return ((Integer) keyParam).longValue();
        } else {
            return 0;
        }
    }

}

