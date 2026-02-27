package com.slg.net.rpc.impl.syncbus;

import com.slg.common.executor.TaskModule;
import com.slg.net.rpc.anno.RpcMethod;
import com.slg.net.rpc.anno.RpcRouteParams;
import com.slg.net.rpc.anno.ThreadKey;
import com.slg.net.rpc.route.impl.ServerIdRoute;

/**
 * 同步总线 RPC 接口
 * 用于跨进程传输同步字段数据
 * <p>
 * 同步为单向 fire-and-forget，无需等待响应
 *
 * @author yangxunan
 * @date 2026/02/12
 */
public interface ISyncBusRpcService {

    /**
     * 接收同步数据
     *
     * @param targetServerId 目标服务器 ID（用于路由）
     * @param entityId       实体唯一标识（用于线程分派和 Cache 查找）
     * @param syncModuleId   同步模块 ID（SyncModule.getId()）
     * @param fieldData      JSON 格式的 Map&lt;String, String&gt; 字符串，所有字段值统一编码为 String
     */
    @RpcMethod(routeClz = ServerIdRoute.class, useModule = TaskModule.PLAYER)
    void receiveSyncData(@RpcRouteParams int targetServerId,
                         @ThreadKey long entityId,
                         int syncModuleId,
                         String fieldData);
}
