package com.slg.net.rpc.impl.web;

import com.slg.net.rpc.anno.RpcMethod;
import com.slg.net.rpc.anno.RpcRouteParams;

/**
 * Web 服 RPC 接口（由 game 侧调用）
 * game 服在创角、登出等关键节点通过此接口通知 web 服更新账号/角色状态
 *
 * <p>路由说明：使用默认的 {@link com.slg.net.rpc.route.impl.ServerIdRoute}，
 * 调用方（game）传入 webServerId，框架路由到对应 web 服的 RPC 连接。
 *
 * @author yangxunan
 * @date 2026-03-02
 */
public interface IWebRpcService {

    /**
     * 创角回调
     * game 服创建角色成功后调用，web 服据此写入 UserEntity 并更新 Account.roleInfoList
     * 设计为幂等：同一 roleId 重复调用时只更新，不重复创建
     *
     * @param webServerId web 服 ID（路由参数）
     * @param accountId   Account 主键
     * @param roleId      game 侧生成的角色 ID，作为 UserEntity 主键
     * @param gameServerId 角色所在 game 服 ID
     */
    @RpcMethod
    void onRoleCreated(@RpcRouteParams int webServerId, long accountId, long roleId, long gameServerId);

    /**
     * 角色登出回调
     * game 服玩家下线后调用，web 服据此更新 UserEntity 中的登出时间等字段
     *
     * @param webServerId    web 服 ID（路由参数）
     * @param roleId         角色 ID
     * @param lastLoginTime  本次登录时间戳（毫秒），用于更新 UserEntity.lastLoginTime
     */
    @RpcMethod
    void onRoleLogout(@RpcRouteParams int webServerId, long roleId, long lastLoginTime);
}
