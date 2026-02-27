package com.slg.net.crossevent.anno;

import com.slg.net.crossevent.model.RouteType;

import java.lang.annotation.*;

/**
 * 跨服事件路由注解：按玩家所在 Game 服路由
 * 标注在事件 VO 的 long 类型字段上，表示该事件发往玩家所在的 Game 服务器
 *
 * <p>对应路由策略：{@code PlayerGameRoute}
 * <p>接收端 TaskModule：{@code PLAYER}（多链，按 playerId 隔离串行）
 * <p>同时作为 ThreadKey
 *
 * <p>使用示例：
 * <pre>
 * public class HeroLevelUpEventVO implements IEvent {
 *     &#64;RoutePlayerGame
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
@CrossRoute(RouteType.PLAYER_GAME)
public @interface RoutePlayerGame {
}
