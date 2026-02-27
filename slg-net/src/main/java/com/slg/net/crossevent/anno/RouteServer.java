package com.slg.net.crossevent.anno;

import com.slg.net.crossevent.model.RouteType;

import java.lang.annotation.*;

/**
 * 跨服事件路由注解：按服务器 ID 路由
 * 标注在事件 VO 的 int 类型字段上，表示该事件发往指定 serverId 的服务器
 *
 * <p>对应路由策略：{@code ServerIdRoute}
 * <p>接收端 TaskModule：{@code SYSTEM}（单链串行）
 * <p>不作为 ThreadKey
 *
 * <p>使用示例：
 * <pre>
 * public class SomeEventVO implements IEvent {
 *     &#64;RouteServer
 *     private int serverId;
 * }
 * </pre>
 *
 * @author yangxunan
 * @date 2026/02/13
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@CrossRoute(RouteType.SERVER)
public @interface RouteServer {
}
