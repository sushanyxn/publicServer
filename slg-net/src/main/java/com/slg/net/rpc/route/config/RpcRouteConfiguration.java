package com.slg.net.rpc.route.config;

import com.slg.net.enable.RpcRouteMarkerConfiguration;
import com.slg.net.rpc.facade.RpcRedisFacade;
import com.slg.net.rpc.manager.RpcCallBackManager;
import com.slg.net.rpc.manager.RpcProxyManager;
import com.slg.net.rpc.route.IRpcRouteSupportService;
import com.slg.net.rpc.route.impl.RedisRoute;
import com.slg.net.rpc.route.redis.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * RPC Redis 路由核心配置类
 * 由 {@link com.slg.net.rpc.anno.EnableRpcRoute} 通过 @Import 引入时，会先加载
 * {@link RpcRouteMarkerConfiguration} 产生标记 Bean，本配置才生效；
 * 若仅被 ComponentScan 扫到（如 SceneMain 扫 com.slg.net.rpc）而未使用 @EnableRpcRoute，
 * 则无标记 Bean，本配置不生效，不会创建 Redis 路由相关 Bean。
 *
 * @author yangxunan
 * @date 2026/03/04
 */
@Configuration
@ConditionalOnBean(RpcRouteMarkerConfiguration.RpcRouteEnabledMarker.class)
@Import(RouteRedisAutoConfiguration.class)
public class RpcRouteConfiguration {

    /**
     * RedisRoute Bean（AbstractRpcRoute 实现）
     * RPC 方法使用 @RpcMethod(routeClz = RedisRoute.class) 时由此 Bean 提供路由
     */
    @Bean
    public RedisRoute redisRoute() {
        return new RedisRoute();
    }

    /**
     * Redis 路由消息发布器
     *
     * @param routeRedisTemplate      转发专用 RedisTemplate（ByteArray 值）
     * @param rpcRouteSupportService  Rpc Route 专用服务接口
     * @param properties              rpc.route.redis.* 配置
     */
    @Bean
    public RedisRoutePublisher redisRoutePublisher(
            @Qualifier("routeRedisTemplate") RedisTemplate<String, byte[]> routeRedisTemplate,
            IRpcRouteSupportService rpcRouteSupportService,
            RpcRouteRedisProperties properties) {
        return new RedisRoutePublisher(routeRedisTemplate, rpcRouteSupportService, properties);
    }

    /**
     * Redis RPC 门面
     *
     * @param rpcProxyManager        方法元数据注册表
     * @param callBackManager        回调管理器
     * @param redisRoutePublisher    Redis 发布器（用于发回响应）
     */
    @Bean
    public RpcRedisFacade rpcRedisFacade(RpcProxyManager rpcProxyManager,
                                          RpcCallBackManager callBackManager,
                                          RedisRoutePublisher redisRoutePublisher) {
        return new RpcRedisFacade(rpcProxyManager, callBackManager, redisRoutePublisher);
    }

    /**
     * Redis 路由消费者运行器（SmartLifecycle）
     * 启动后在虚拟线程中持续消费请求 Stream 和响应 Stream
     *
     * @param routeRedisTemplate     转发专用 RedisTemplate
     * @param rpcRedisFacade         Redis RPC 门面
     * @param rpcRouteSupportService Rpc Route 专用服务接口
     * @param properties             rpc.route.redis.* 配置
     */
    @Bean
    public RpcRedisRouteConsumerRunner rpcRedisRouteConsumerRunner(
            @Qualifier("routeRedisTemplate") RedisTemplate<String, byte[]> routeRedisTemplate,
            RpcRedisFacade rpcRedisFacade,
            IRpcRouteSupportService rpcRouteSupportService,
            RpcRouteRedisProperties properties) {
        return new RpcRedisRouteConsumerRunner(routeRedisTemplate, rpcRedisFacade,
                rpcRouteSupportService, properties);
    }
}
