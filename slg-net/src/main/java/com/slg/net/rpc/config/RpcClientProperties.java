package com.slg.net.rpc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RPC 客户端配置属性
 *
 * @author yangxunan
 * @date 2026/02/10
 */
@ConfigurationProperties(prefix = "rpc.client")
@Getter
@Setter
public class RpcClientProperties {

    /**
     * {@link com.slg.net.rpc.route.IRouteSupportService} 实现类的全限定类名
     * 必须配置，否则 RPC 客户端路由功能无法正常工作
     *
     * <p>示例：
     * <pre>
     * rpc:
     *   client:
     *     route-service-class: com.slg.game.net.rpc.GameRpcRouteService
     * </pre>
     */
    private String routeServiceClass;

}
