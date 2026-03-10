package com.slg.game.base.login.service;

import com.slg.common.constant.LoginCode;
import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.game.base.player.manager.PlayerManager;
import com.slg.game.base.player.model.Player;
import com.slg.game.base.player.model.SceneServerContext;
import com.slg.game.base.player.model.SceneServerContext.ConnectState;
import com.slg.game.base.player.service.PlayerService;
import com.slg.game.net.ToClientPacketUtil;
import com.slg.game.net.manager.PlayerSessionManager;
import com.slg.net.message.clientmessage.login.packet.CM_LoginReq;
import com.slg.net.message.clientmessage.login.packet.SM_LoginResp;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 登录服务
 * 处理玩家登录和登出逻辑
 *
 * @author yangxunan
 * @date 2026/1/22
 */
@Component
public class LoginService {

    @Autowired
    private PlayerManager playerManager;
    @Autowired
    private PlayerSessionManager playerSessionManager;
    @Autowired
    private PlayerService playerService;

    /**
     * 玩家登录
     * 登录成功后检查 sceneInit 状态，按需异步补初始化
     */
    public void login(NetSession session, CM_LoginReq loginReq) {
        if (loginReq.getPlayerId() <= 0) {
            return;
        }
        Player player = playerManager.getPlayer(loginReq.getPlayerId());
        if (player != null) {

            if (player.getSession() != null) {
                // 清理旧的session
                logout(player.getSession());
            }
            playerSessionManager.bindPlayer(session, player);
            ToClientPacketUtil.send(player, SM_LoginResp.valueOf(LoginCode.SUCCESS, player.getId()));

            // 登录后检查场景初始化状态，按需补初始化
            // sceneInit 是玩家场景是否就绪的唯一判断依据（ConnectState 仅表示服务器连接状态）
            if (!player.getSceneContext().isSceneInit()) {
                SceneServerContext ctx = playerManager.getSceneServerContextMap()
                        .get(player.getPlayerEntity().getSceneServerId());
                if (ctx != null && ctx.getConnectState().get() == ConnectState.READY) {
                    // Scene 连接正常但玩家未初始化，异步补初始化（不阻塞登录）
                    LoggerUtil.debug("[登录] 玩家{}场景未初始化，异步补初始化", player.getId());
                    Executor.Player.execute(player.getId(), () -> {
                        playerService.initPlayerScene(player.getId());
                    });
                }
                // 如果 Scene 连接不可用，等待连接恢复后由 batchInitPlayerScene 处理
            }
        } else {
            session.close("未找到玩家" + loginReq.getPlayerId());
        }
    }

    /**
     * 玩家登出
     * 含防御性判空；若会话已被释放但未关闭，会补充关闭。
     */
    public void logout(NetSession session){
        if (session == null) {
            return;
        }
        if (session.getPlayerId() <= 0) {
            session.close("未注册连接登出");
            return;
        }
        Player player = playerManager.getPlayer(session.getPlayerId());
        if (player == null || player.getSession() != session) {
            // 会话已释放（玩家不存在或已绑定其他 session）但未关闭，补充关闭
            session.close("会话已释放或玩家不存在");
            return;
        }
        playerSessionManager.unbindPlayer(session);
        session.close("玩家" + player.getId() + "登出, 链接已解除绑定");
    }

}
