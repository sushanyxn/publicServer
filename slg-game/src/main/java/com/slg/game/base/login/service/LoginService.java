package com.slg.game.base.login.service;

import com.slg.common.constant.LoginCode;
import com.slg.common.event.manager.EventBusManager;
import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.game.base.account.manager.AccountManager;
import com.slg.game.base.account.model.AccountRoleBrief;
import com.slg.game.base.login.event.PlayerLoginEvent;
import com.slg.game.base.login.event.PlayerLogoutEvent;
import com.slg.game.base.model.GameIdCreate;
import com.slg.game.base.player.entity.PlayerEntity;
import com.slg.game.base.player.manager.PlayerManager;
import com.slg.game.base.player.model.Player;
import com.slg.game.base.player.model.SceneServerContext;
import com.slg.game.base.player.model.SceneServerContext.ConnectState;
import com.slg.game.base.player.service.PlayerService;
import com.slg.game.net.ToClientPacketUtil;
import com.slg.game.net.manager.PlayerSessionManager;
import com.slg.net.message.clientmessage.login.packet.CM_LoginFinish;
import com.slg.net.message.clientmessage.login.packet.CM_LoginReq;
import com.slg.net.message.clientmessage.login.packet.SM_LoginResp;
import com.slg.net.socket.model.NetSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 登录服务
 * 处理玩家登录和登出逻辑。完整登录流程：CM_LoginReq -> SM_LoginResp -> CM_LoginFinish（正式登录时在玩家线程抛登录事件）
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
    @Autowired
    private AccountManager accountManager;

    /**
     * 玩家登录
     * 根据 CM_LoginReq 选择角色：账号不存在则创建，角色不存在则创建；顶号后绑定会话并下发 SM_LoginResp
     */
    public void login(NetSession session, CM_LoginReq loginReq) {
        if (loginReq.getAccount() == null || loginReq.getAccount().isBlank()) {
            session.sendMessage(SM_LoginResp.valueOf(LoginCode.FAIL_ACCOUNT_EMPTY, 0));
            session.close("账号为空");
            return;
        }
        String accountId = loginReq.getAccount().trim();
        accountManager.getOrCreate(accountId);

        long roleId;
        if (loginReq.getPlayerId() > 0) {
            if (!accountManager.existsRole(accountId, loginReq.getPlayerId())) {
                session.sendMessage(SM_LoginResp.valueOf(LoginCode.FAIL_ROLE_NOT_IN_ACCOUNT, 0));
                session.close("指定角色不属于该账号");
                return;
            }
            roleId = loginReq.getPlayerId();
        } else {
            Optional<AccountRoleBrief> lastRole = accountManager.getLastLoginRole(accountId);
            if (lastRole.isPresent()) {
                roleId = lastRole.get().getRoleId();
            } else {
                roleId = GameIdCreate.PLAYER.nextId();
                accountManager.updateRoleLoginTime(accountId, roleId, LocalDateTime.now());
            }
        }

        Player player = playerManager.getPlayer(roleId);
        if (player != null) {
            // 角色已初始化，直接在本登录链内完成绑定与下发
            if (player.getSession() != null) {
                logout(player.getSession());
            }
            playerSessionManager.bindPlayer(session, player);
            accountManager.updateRoleLoginTime(accountId, roleId, LocalDateTime.now());
            ToClientPacketUtil.send(player, SM_LoginResp.valueOf(LoginCode.SUCCESS, player.getId()));
            if (!player.getSceneContext().isSceneInit()) {
                SceneServerContext ctx = playerManager.getSceneServerContextMap()
                        .get(player.getPlayerEntity().getSceneServerId());
                if (ctx != null && ctx.getConnectState().get() == ConnectState.READY) {
                    LoggerUtil.debug("[登录] 玩家{}场景未初始化，异步补初始化", player.getId());
                    Executor.Player.execute(player.getId(), () -> playerService.initPlayerScene(player.getId()));
                }
            }
            return;
        }

        // 需在玩家线程中加载/创角，完成后重新进入 login 走绑定与下发（此时 getPlayer(roleId) 已非空）
        Executor.Player.submit(roleId, () -> loadAndInitPlayerInPlayerThread(roleId, accountId))
                .whenComplete((p, ex) -> Executor.Login.execute(() -> {
                    if (session == null || !session.isActive()) {
                        LoggerUtil.debug("[登录] 异步初始化完成前连接已关闭 accountId={}, roleId={}", accountId, roleId);
                        return;
                    }
                    if (ex != null) {
                        LoggerUtil.error("[登录] 角色加载异常 accountId={}, roleId={}", accountId, roleId, ex);
                        session.sendMessage(SM_LoginResp.valueOf(LoginCode.FAIL_ROLE_NOT_IN_ACCOUNT, 0));
                        session.close("角色加载失败");
                        return;
                    }
                    if (p == null) {
                        session.sendMessage(SM_LoginResp.valueOf(LoginCode.FAIL_ROLE_NOT_IN_ACCOUNT, 0));
                        session.close("角色加载失败");
                        return;
                    }
                    login(session, loginReq);
                }));
    }

    /**
     * 客户端正式登录完成（CM_LoginFinish）
     * 在玩家线程中调用，抛出登录事件等，后续可在此扩展更多逻辑
     */
    public void loginFinish(NetSession session, CM_LoginFinish packet, Player player) {
        EventBusManager.getInstance().publishEvent(PlayerLoginEvent.valueOf(player));
    }

    /**
     * 在玩家线程中加载并初始化玩家（仅允许在玩家线程调用）
     * 无实体则创角，有实体则补建 Player 并做 Game 初始化与场景上下文登记
     *
     * @param roleId   角色 ID
     * @param accountId 账号 ID
     * @return 初始化后的 Player，失败返回 null
     */
    private Player loadAndInitPlayerInPlayerThread(long roleId, String accountId) {
        PlayerEntity entity = playerManager.getPlayerEntityCache().findById(roleId);
        if (entity == null) {
            playerService.createNewPlayer(roleId, accountId);
            return playerManager.getPlayer(roleId);
        }
        Player player = new Player(entity);
        playerService.initPlayerGame(player);
        playerService.addPlayerToSceneContext(player);
        return player;
    }

    /**
     * 玩家登出
     * 在玩家线程抛出登出事件后，解绑并关闭连接
     */
    public void logout(NetSession session) {
        if (session == null) {
            return;
        }
        if (session.getPlayerId() <= 0) {
            session.close("未注册连接登出");
            return;
        }
        Player player = playerManager.getPlayer(session.getPlayerId());
        if (player == null || player.getSession() != session) {
            session.close("会话已释放或玩家不存在");
            return;
        }
        long playerId = session.getPlayerId();
        Executor.Player.submit(playerId, () -> {
            EventBusManager.getInstance().publishEvent(PlayerLogoutEvent.valueOf(player));
            return null;
        }).thenRun(() -> Executor.Login.execute(() -> {
            playerSessionManager.unbindPlayer(session);
            session.close("玩家" + playerId + "登出, 链接已解除绑定");
        }));
    }
}
