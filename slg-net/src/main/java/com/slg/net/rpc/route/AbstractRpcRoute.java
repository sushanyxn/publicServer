package com.slg.net.rpc.route;

import com.slg.net.message.innermessage.rpc.packet.IM_RpcRequest;
import com.slg.net.rpc.anno.RpcRouteParams;
import com.slg.net.rpc.model.RpcMethodMeta;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * RPC 路由抽象类
 * 负责判断路由目标和发送消息
 *
 * @author yangxunan
 * @date 2026/01/23
 */
public abstract class AbstractRpcRoute {

    @Autowired
    protected IRouteSupportService routeSupportService;

    /**
     * 检查 RPC 方法中定义的路由参数 {@link RpcRouteParams} 是否满足路由器的要求
     *
     * @param methodMeta 方法元数据
     * @return true 表示参数定义正确
     */
    public boolean verifyRouteParamsDefine(RpcMethodMeta methodMeta) {
        Class<?>[] routeParams = getRouteParams();
        if (routeParams == null || routeParams.length == 0) {
            return true;
        }
        if (methodMeta.getRouteParamsIndex().length != routeParams.length) {
            return false;
        }
        
        // 验证参数类型匹配
        for (int i = 0; i < routeParams.length; i++) {
            Class<?> paramType = methodMeta.getMethod().getParameterTypes()[methodMeta.getRouteParamsIndex()[i]];
            if (paramType != routeParams[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取路由所需的参数类型
     *
     * @return 参数类型数组
     */
    public abstract Class<?>[] getRouteParams();

    /**
     * 发送 RPC 请求消息
     *
     * @param request       请求消息
     * @param rpcMethodMeta
     * @param params        方法参数（用于提取路由参数）
     */
    public abstract void sendMsg(IM_RpcRequest request, RpcMethodMeta rpcMethodMeta, Object... params);

    /**
     * 判断是否是本地调用
     *
     * @param rpcMethodMeta 方法元数据
     * @param params        方法参数
     * @return true 表示本地调用
     */
    public abstract boolean isLocal(RpcMethodMeta rpcMethodMeta, Object... params);

    /**
     * 解析目标服务器ID（用于 RPC 回调的断线清理）
     * 子类可覆写以提供准确的目标 serverId
     *
     * @param rpcMethodMeta 方法元数据
     * @param routeParams   路由参数（已提取）
     * @return 目标服务器ID，0 表示未知
     */
    public int resolveTargetServerId(RpcMethodMeta rpcMethodMeta, Object... routeParams) {
        return 0;
    }

}
