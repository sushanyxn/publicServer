package com.slg.net.rpc.manager;

import com.slg.net.rpc.model.RpcMethodMeta;
import com.slg.net.rpc.proxy.CglibProxy;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 代理对象管理器
 * 负责管理 RPC 方法元数据和代理对象缓存
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Component
public class RpcProxyManager {

    private final CglibProxy cglibProxy;

    public RpcProxyManager(@Lazy CglibProxy cglibProxy) {
        this.cglibProxy = cglibProxy;

    }

    @Getter
    private static RpcProxyManager instance;

    /**
     * 方法元数据映射：methodMarker -> RpcMethodMeta
     */
    private final Map<String, RpcMethodMeta> methodMetaMap = new ConcurrentHashMap<>();

    /**
     * 代理对象缓存：interfaceClass -> proxyObject
     */
    private final Map<Class<?>, Object> proxyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init(){
        instance = this;
    }

    /**
     * 注册 RPC 方法元数据
     *
     * @param methodMarker 方法标识
     * @param meta         方法元数据
     */
    public void registerMethodMeta(String methodMarker, RpcMethodMeta meta){
        methodMetaMap.put(methodMarker, meta);
    }

    /**
     * 获取 RPC 方法元数据
     *
     * @param methodMarker 方法标识
     * @return 方法元数据，不存在则返回 null
     */
    public RpcMethodMeta getRpcMethodMeta(String methodMarker){
        return methodMetaMap.get(methodMarker);
    }

    /**
     * 获取或创建代理对象
     * 如果缓存中不存在，则创建新的代理对象并缓存
     *
     * @param interfaceClass 接口类
     * @param <T>            接口类型
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCreateProxy(Class<T> interfaceClass){
        return (T) proxyCache.computeIfAbsent(interfaceClass, clz -> {
            Object proxy = cglibProxy.createProxy((Class<T>) clz);
            return proxy;
        });
    }

    /**
     * 获取已注册的方法数量（用于监控）
     */
    public int getMethodCount(){
        return methodMetaMap.size();
    }

    /**
     * 获取已缓存的代理对象数量（用于监控）
     */
    public int getProxyCount(){
        return proxyCache.size();
    }

}
