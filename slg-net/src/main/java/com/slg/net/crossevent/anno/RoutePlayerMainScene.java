package com.slg.net.crossevent.anno;

import com.slg.net.crossevent.model.RouteType;

import java.lang.annotation.*;

/**
 * 跨服事件路由注解：按玩家主场景服路由
 * 标注在事件 VO 的 long 类型字段上，表示该事件发往玩家所在的主场景服务器
 *
 * <p>对应路由策略：{@code PlayerMainSceneRoute}
 * <p>接收端 TaskModule：{@code PLAYER}（多链，按 playerId 隔离串行）
 * <p>同时作为 ThreadKey
 *
 * <p>使用示例：
 * <pre>
 * public class SomeEventVO implements IEvent {
 *     &#64;RoutePlayerMainScene
 *     private long playerId;
 * }
 * </pre>
 *
 * @author yangxunan
 * @date 2026/02/13
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@CrossRoute(RouteType.PLAYER_MAIN_SCENE)
public @interface RoutePlayerMainScene {
}
