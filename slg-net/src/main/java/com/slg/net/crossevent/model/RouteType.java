package com.slg.net.crossevent.model;

import com.slg.net.crossevent.rpc.ICrossServerEventRpcService;

import java.lang.invoke.MethodHandle;
import java.util.Set;

/**
 * 跨服事件路由类型枚举
 * 每个枚举值自包含字段类型校验与 RPC 分发逻辑，消除外部 if-else / switch
 *
 * <p>新增路由类型只需：
 * <ol>
 *   <li>新增枚举值（声明字段类型 + 实现 dispatch）</li>
 *   <li>新建路由注解并打上 {@code @CrossRoute(RouteType.XXX)} 元注解</li>
 *   <li>在 {@link ICrossServerEventRpcService} 中新增对应 RPC 方法</li>
 * </ol>
 *
 * @author yangxunan
 * @date 2026/02/13
 */
public enum RouteType {

    /**
     * 按服务器 ID 路由
     * 对应注解 @RouteServer，字段类型 int
     */
    SERVER(Set.of(int.class, Integer.class)) {
        @Override
        public void dispatch(ICrossServerEventRpcService rpc, MethodHandle getter, Object vo) throws Throwable {
            rpc.dispatchByServer((int) getter.invoke(vo), vo);
        }
    },

    /**
     * 按玩家所在 Game 服路由
     * 对应注解 @RoutePlayerGame，字段类型 long
     */
    PLAYER_GAME(Set.of(long.class, Long.class)) {
        @Override
        public void dispatch(ICrossServerEventRpcService rpc, MethodHandle getter, Object vo) throws Throwable {
            rpc.dispatchByPlayerGame((long) getter.invoke(vo), vo);
        }
    },

    /**
     * 按玩家主场景服路由
     * 对应注解 @RoutePlayerMainScene，字段类型 long
     */
    PLAYER_MAIN_SCENE(Set.of(long.class, Long.class)) {
        @Override
        public void dispatch(ICrossServerEventRpcService rpc, MethodHandle getter, Object vo) throws Throwable {
            rpc.dispatchByPlayerMainScene((long) getter.invoke(vo), vo);
        }
    },

    /**
     * 按玩家当前场景服路由
     * 对应注解 @RoutePlayerCurrentScene，字段类型 long
     */
    PLAYER_CURRENT_SCENE(Set.of(long.class, Long.class)) {
        @Override
        public void dispatch(ICrossServerEventRpcService rpc, MethodHandle getter, Object vo) throws Throwable {
            rpc.dispatchByPlayerCurrentScene((long) getter.invoke(vo), vo);
        }
    };

    /**
     * 该路由类型接受的字段类型集合
     */
    private final Set<Class<?>> acceptedFieldTypes;

    RouteType(Set<Class<?>> acceptedFieldTypes) {
        this.acceptedFieldTypes = acceptedFieldTypes;
    }

    /**
     * 校验字段类型是否与此路由类型匹配
     *
     * @param fieldType 字段类型
     * @return true 表示类型匹配
     */
    public boolean isValidFieldType(Class<?> fieldType) {
        return acceptedFieldTypes.contains(fieldType);
    }

    /**
     * 从 VO 中提取路由参数并调用对应的 RPC 分发方法
     *
     * @param rpc    RPC 服务代理
     * @param getter 路由字段的 MethodHandle getter
     * @param vo     事件 VO 对象
     * @throws Throwable MethodHandle 调用异常
     */
    public abstract void dispatch(ICrossServerEventRpcService rpc, MethodHandle getter, Object vo) throws Throwable;
}
