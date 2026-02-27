package com.slg.net.crossevent;

import com.slg.common.event.model.IEvent;

/**
 * 跨服事件接口
 * 实现此接口的事件在本地发布后，会自动通过 RPC 转发到目标服务器
 *
 * <p>使用方式：
 * <ol>
 *   <li>业务事件类实现此接口</li>
 *   <li>覆写 {@link #toCrossEvent()} 方法，使用<b>协变返回类型</b>声明具体 VO 类</li>
 *   <li>VO 类位于 {@code com.slg.net.message} 包下，并实现 {@link IEvent}</li>
 *   <li>VO 类的路由字段标注对应路由注解（如 {@code @RoutePlayerGame}）</li>
 * </ol>
 *
 * <p>示例：
 * <pre>
 * public class HeroLevelUpEvent implements ICrossServerEvent {
 *     // 必须使用协变返回类型，不能写 IEvent
 *     &#64;Override
 *     public HeroLevelUpEventVO toCrossEvent() {
 *         HeroLevelUpEventVO vo = new HeroLevelUpEventVO();
 *         vo.setPlayerId(player.getId());
 *         vo.setHeroId(heroId);
 *         vo.setLevel(level);
 *         return vo;
 *     }
 * }
 * </pre>
 *
 * <p>返回类型校验规则（启动时由 {@code CrossServerEventBridge} 执行）：
 * <ul>
 *   <li>必须声明具体的协变返回类型，不能是 {@code IEvent} 或 {@code Object}</li>
 *   <li>返回类型必须是非抽象、非接口的具体类</li>
 *   <li>返回类型必须位于 {@code com.slg.net.message} 包（或其子包）下</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/02/13
 * @see com.slg.net.crossevent.bridge.CrossServerEventBridge
 */
public interface ICrossServerEvent extends IEvent {

    /**
     * 转换为可跨服传播的事件
     * <p>子类必须使用协变返回类型声明具体的返回类，例如：
     * <pre>
     * public HeroLevelUpEventVO toCrossEvent() { ... }
     * </pre>
     * <p>返回类型必须满足：
     * <ul>
     *   <li>非抽象、非接口的具体类</li>
     *   <li>位于 com.slg.net.message 包（或其子包）下</li>
     * </ul>
     * <p>返回 null 表示本次不需要跨服传播
     *
     * @return 跨服事件 VO，返回 null 表示不转发
     */
    IEvent toCrossEvent();
}
