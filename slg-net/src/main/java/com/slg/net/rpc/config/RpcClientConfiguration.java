package com.slg.net.rpc.config;

import com.slg.common.log.LoggerUtil;
import com.slg.net.rpc.route.IRouteSupportService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RPC 客户端配置类
 * 根据 yml 中配置的 {@code rpc.client.route-service-class} 实例化并注册 {@link IRouteSupportService}
 *
 * <p>该配置类位于 {@code com.slg.net.rpc} 包下，随 RPC 包扫描自动生效。
 * 扫描了 RPC 包即引入客户端能力，必须在 yml 中配置路由服务实现类，否则启动报错。
 *
 * @author yangxunan
 * @date 2026/02/10
 */
@Configuration
@EnableConfigurationProperties(RpcClientProperties.class)
public class RpcClientConfiguration {

    /**
     * 根据 yml 配置创建并注册 {@link IRouteSupportService} 实例
     * Spring 会自动处理实例内的 {@code @Autowired} 等注解注入
     *
     * @param properties  RPC 客户端配置属性
     * @param beanFactory Spring Bean 工厂，用于创建并自动装配实例
     * @return 路由服务实例
     */
    @Bean
    public IRouteSupportService routeSupportService(RpcClientProperties properties,
                                                    AutowireCapableBeanFactory beanFactory) {
        String className = properties.getRouteServiceClass();

        // 校验配置是否存在
        if (className == null || className.isBlank()) {
            throw new IllegalStateException(
                    "[RPC] 必须在 application.yml 中配置 rpc.client.route-service-class，" +
                    "指定 IRouteSupportService 的实现类全限定类名");
        }

        try {
            // 加载类
            Class<?> clazz = Class.forName(className);

            // 校验是否实现了 IRouteSupportService 接口
            if (!IRouteSupportService.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException(
                        "[RPC] rpc.client.route-service-class 配置的类 " + className +
                        " 未实现 IRouteSupportService 接口");
            }

            // 通过 BeanFactory 创建实例，自动处理 @Autowired 等依赖注入
            IRouteSupportService service = (IRouteSupportService) beanFactory.createBean(clazz);
            LoggerUtil.debug("[RPC] 路由服务初始化完成: {}", className);
            return service;

        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "[RPC] rpc.client.route-service-class 配置的类 " + className + " 未找到", e);
        }
    }

}
