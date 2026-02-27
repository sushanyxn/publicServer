package com.slg.net.crossevent.model;

import com.slg.net.crossevent.anno.CrossRoute;
import com.slg.net.crossevent.rpc.ICrossServerEventRpcService;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * 跨服事件 VO 路由元数据
 * 启动时一次性解析 VO 类的路由注解并缓存，运行时零反射
 *
 * <p>每个事件 VO 类有且仅有一个路由注解字段。
 * 解析阶段同时校验路由注解的存在性和唯一性。
 *
 * <p>通过 {@link CrossRoute} 元注解自动识别路由类型，
 * 新增路由类型无需修改此类。
 *
 * @author yangxunan
 * @date 2026/02/13
 */
@Getter
public class CrossServerEventMeta {

    /**
     * 路由类型
     */
    private RouteType routeType;

    /**
     * 路由字段的 MethodHandle getter（高效反射）
     */
    private MethodHandle fieldGetter;

    /**
     * 扫描 VO 类的字段注解，构建路由元数据
     * 同时校验路由注解是否存在且唯一
     *
     * @param voClass 事件 VO 类
     * @return 路由元数据
     * @throws IllegalStateException 路由注解缺失、重复或字段类型不匹配
     */
    public static CrossServerEventMeta resolve(Class<?> voClass) {
        CrossServerEventMeta meta = new CrossServerEventMeta();
        Field routeField = null;
        RouteType foundType = null;

        // 遍历 VO 类的所有字段（包括父类字段）
        for (Class<?> clz = voClass; clz != null && clz != Object.class; clz = clz.getSuperclass()) {
            for (Field field : clz.getDeclaredFields()) {
                RouteType type = resolveFieldRouteType(field);
                if (type == null) {
                    continue;
                }

                // 校验唯一性：一个 VO 只能有一个路由注解
                if (foundType != null) {
                    throw new IllegalStateException(
                            voClass.getSimpleName() + " 存在多个路由注解字段: " +
                                    routeField.getName() + " 和 " + field.getName() +
                                    "，每个事件 VO 类有且仅有一个路由注解字段");
                }

                // 校验字段类型 —— 委托给 RouteType 自身
                if (!type.isValidFieldType(field.getType())) {
                    throw new IllegalStateException(
                            voClass.getSimpleName() + "." + field.getName() +
                                    " 标注了 " + type.name() + " 路由，字段类型必须是 " +
                                    type.name() + " 支持的类型，当前类型: " + field.getType().getSimpleName());
                }

                foundType = type;
                routeField = field;
            }
        }

        // 校验存在性
        if (foundType == null) {
            throw new IllegalStateException(
                    voClass.getSimpleName() + " 缺少路由注解字段，" +
                            "必须在某个字段上标注带有 @CrossRoute 元注解的路由注解（如 @RouteServer、@RoutePlayerGame 等）");
        }

        meta.routeType = foundType;

        // 构建 MethodHandle getter，提升运行时访问效率
        try {
            routeField.setAccessible(true);
            meta.fieldGetter = MethodHandles.lookup().unreflectGetter(routeField);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "无法创建字段访问句柄: " + voClass.getSimpleName() + "." + routeField.getName(), e);
        }

        return meta;
    }

    /**
     * 通过元注解 {@link CrossRoute} 解析字段上的路由类型
     * 遍历字段上的所有注解，检查注解自身是否标注了 @CrossRoute
     *
     * @param field 字段
     * @return 路由类型，无路由注解返回 null
     */
    private static RouteType resolveFieldRouteType(Field field) {
        for (Annotation anno : field.getAnnotations()) {
            CrossRoute crossRoute = anno.annotationType().getAnnotation(CrossRoute.class);
            if (crossRoute != null) {
                return crossRoute.value();
            }
        }
        return null;
    }

    /**
     * 执行分发：从 VO 中提取路由参数并调用对应的 RPC 方法
     * 分发逻辑由 {@link RouteType#dispatch} 内聚实现
     *
     * @param rpc RPC 服务代理
     * @param vo  事件 VO 对象
     */
    public void dispatch(ICrossServerEventRpcService rpc, Object vo) {
        try {
            routeType.dispatch(rpc, fieldGetter, vo);
        } catch (Throwable e) {
            throw new RuntimeException(
                    "跨服事件分发失败: " + vo.getClass().getSimpleName() + ", routeType=" + routeType, e);
        }
    }
}
