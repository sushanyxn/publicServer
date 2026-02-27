package com.slg.web.gm.shiro;

import com.slg.web.gm.shiro.filter.AjaxFormAuthenticationFilter;
import com.slg.web.gm.shiro.filter.AjaxPermissionsAuthorizationFilter;
import com.slg.web.gm.shiro.filter.AjaxRolesAuthorizationFilter;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.spring.web.config.DefaultShiroFilterChainDefinition;
import org.apache.shiro.spring.web.config.ShiroFilterChainDefinition;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shiro 安全框架 Spring Boot 配置
 * 利用 Shiro Spring Boot Starter 的自动配置机制：
 * - SecurityManager、ShiroFilterFactoryBean 由自动配置创建
 * - 只需提供 Realm 和 FilterChainDefinition
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Configuration
public class ShiroConfig {

    @Bean
    public Realm authRealm() {
        return new AuthRealm();
    }

    @Bean
    public ShiroFilterChainDefinition shiroFilterChainDefinition() {
        DefaultShiroFilterChainDefinition chain = new DefaultShiroFilterChainDefinition();
        chain.addPathDefinition("/pstatic/**", "anon");
        chain.addPathDefinition("/api/**", "anon");
        chain.addPathDefinition("/gm/login", "anon");
        chain.addPathDefinition("/gm/doLogin", "anon");
        chain.addPathDefinition("/gm/health", "anon");
        chain.addPathDefinition("/gm/**", "authc");
        return chain;
    }

    /**
     * 注册 Ajax 认证过滤器，替换默认的 FormAuthenticationFilter
     * 对 Ajax 请求返回 JSON 而非重定向
     */
    @Bean
    public FilterRegistrationBean<AjaxFormAuthenticationFilter> ajaxFormAuthFilter() {
        FilterRegistrationBean<AjaxFormAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AjaxFormAuthenticationFilter());
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AjaxRolesAuthorizationFilter> ajaxRolesAuthFilter() {
        FilterRegistrationBean<AjaxRolesAuthorizationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AjaxRolesAuthorizationFilter());
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AjaxPermissionsAuthorizationFilter> ajaxPermsAuthFilter() {
        FilterRegistrationBean<AjaxPermissionsAuthorizationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AjaxPermissionsAuthorizationFilter());
        registration.setEnabled(false);
        return registration;
    }
}
