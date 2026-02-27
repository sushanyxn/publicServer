package com.slg.net.rpc.model;

import com.slg.common.executor.TaskModule;
import com.slg.net.rpc.route.AbstractRpcRoute;
import lombok.Getter;
import lombok.Setter;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * RPC 方法元数据
 * 包含 RPC 方法的所有必要信息，用于方法调用和路由
 *
 * @author yangxunan
 * @date 2026/01/23
 */
@Getter
@Setter
public class RpcMethodMeta {

    /**
     * RPC 方法标识：接口全限定名 + "#" + 方法名
     */
    private String methodMarker;

    /**
     * 原始方法对象
     */
    private Method method;

    /**
     * 返回值类型
     */
    private Class<?> returnType;

    /**
     * RPC 方法参数字段
     */
    private Field[] fields;

    /**
     * 路由参数下标（标注了 @RpcRouteParams 的参数位置）
     */
    private int[] routeParamsIndex;

    /**
     * ThreadKey 参数位置（标注了 @ThreadKey 的参数位置）
     * 默认 -1 表示未设置
     */
    private int threadKeyIndex = -1;

    /**
     * 路由策略类
     */
    private Class<? extends AbstractRpcRoute> routeClass;

    /**
     * 路由实例（从 Spring 容器注入）
     */
    private AbstractRpcRoute routeInstance;

    /**
     * 实现类对象
     * 只有实现了接口才有，只有接口没有实现类是没有的
     */
    private Object bean;

    /**
     * 实现类的方法句柄（用于高性能反射调用）
     * 只有实现了接口才有，只有接口没有实现类是没有的
     */
    private MethodHandle methodHandle;

    /**
     * 任务模块，用于 RPC 请求的线程分派
     * 有实现类的一定要有 taskModule
     */
    private TaskModule taskModule;

    /**
     * RPC 调用超时时间（毫秒）
     * 默认 30000 毫秒（30秒）
     */
    private long timeoutMillis = 30000L;

}
