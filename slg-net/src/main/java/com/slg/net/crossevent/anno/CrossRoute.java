package com.slg.net.crossevent.anno;

import com.slg.net.crossevent.model.RouteType;

import java.lang.annotation.*;

/**
 * 跨服事件路由元注解
 * 标注在路由注解上（如 @RouteServer、@RoutePlayerGame 等），
 * 声明该路由注解对应的 {@link RouteType}，供框架统一扫描识别
 *
 * <p>框架通过读取字段注解上的此元注解来自动解析路由类型，
 * 新增路由类型只需新建注解并打上此元注解即可，无需修改框架核心代码
 *
 * @author yangxunan
 * @date 2026/02/13
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossRoute {

    /**
     * 路由类型
     */
    RouteType value();
}
