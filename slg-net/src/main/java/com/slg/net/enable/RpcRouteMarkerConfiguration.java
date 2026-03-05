package com.slg.net.enable;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RPC Redis 路由“已启用”标记配置
 * 仅由 {@link com.slg.net.rpc.anno.EnableRpcRoute} 通过 @Import 引入，本类位于 enable 包，
 * 不在各进程的 ComponentScan 范围内，故未使用 @EnableRpcRoute 的进程（如 SceneMain）不会加载本类。
 *
 * @author yangxunan
 * @date 2026/03/05
 */
@Configuration
public class RpcRouteMarkerConfiguration {

    /**
     * 标记 Bean：存在即表示已启用 Redis 路由，{@link com.slg.net.rpc.route.config.RpcRouteConfiguration} 据此条件加载。
     */
    @Bean
    public static RpcRouteEnabledMarker rpcRouteEnabledMarker() {
        return new RpcRouteEnabledMarker();
    }

    /** 仅作存在性标记，无行为 */
    public static final class RpcRouteEnabledMarker {}
}
