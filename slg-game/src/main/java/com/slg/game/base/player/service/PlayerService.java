package com.slg.game.base.player.service;

import com.slg.common.constant.OptionResult;
import com.slg.common.executor.Executor;
import com.slg.common.log.LoggerUtil;
import com.slg.game.base.player.entity.PlayerEntity;
import com.slg.game.base.player.manager.PlayerManager;
import com.slg.game.base.player.model.Player;
import com.slg.game.base.player.model.SceneServerContext;
import com.slg.net.rpc.anno.RpcRef;
import com.slg.net.rpc.impl.scene.IScenePlayerDataRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionException;

/**
 * 玩家服务
 * 负责玩家的初始化和场景相关业务
 *
 * @author yangxunan
 * @date 2025/12/23
 */
@Component
public class PlayerService {

    @Autowired
    private PlayerManager playerManager;

    @RpcRef
    private IScenePlayerDataRpcService scenePlayerDataRpcService;

    /**
     * 初始化玩家的 Game 业务数据
     *
     * @param player 玩家对象
     */
    public void initPlayerGame(Player player) {
        // game业务初始化
        player.getPlayerEntity().getMainTaskInfo().init(player);

        // 初始化完成后，纳入playerManager管理，表示活跃且可以被使用
        playerManager.getPlayers().put(player.getId(), player);
    }

    /**
     * 初始化玩家的场景数据
     * 包括绑定/创建 ScenePlayer 和初始化场景节点（主城等）
     * 在 Player 多链中使用 .join() 同步等待（安全，只阻塞当前玩家链）
     *
     * @param playerId 玩家ID
     */
    public void initPlayerScene(long playerId) {
        Player player = playerManager.getPlayers().get(playerId);
        if (player == null) {
            return;
        }

        try {
            // 步骤1: 确保 ScenePlayer 存在
            int result = scenePlayerDataRpcService.bindScenePlayer(player.getId()).join();
            if (result == OptionResult.SCENE_PLAYER_NOT_FOUND) {
                result = scenePlayerDataRpcService.createScenePlayer(player.getId(), player.toScenePlayerVO()).join();
            }

            // 步骤2: 初始化场景节点
            if (result == OptionResult.SUCCESS) {
                result = scenePlayerDataRpcService.initPlayer(player.getId()).join();
            }

            // 步骤3: 标记初始化完成
            if (result == OptionResult.SUCCESS) {
                player.getSceneContext().setSceneInit(true);
            }
        } catch (CompletionException e) {
            LoggerUtil.error("[场景初始化] 玩家{}场景初始化异常", player.getId(), e);
        }
    }

    /**
     * 批量初始化某个场景服上所有玩家的场景数据
     * 在 System 链中调用，逐个在玩家线程中分发 initPlayerScene
     * 个别玩家初始化失败由登录时补偿兜底
     *
     * @param sceneServerId 场景服ID
     */
    public void batchInitPlayerScene(int sceneServerId) {
        SceneServerContext ctx = playerManager.getSceneServerContextMap().get(sceneServerId);
        if (ctx == null) {
            return;
        }

        LoggerUtil.debug("[批量初始化] 开始初始化场景服{}的玩家, 共{}个", sceneServerId, ctx.getPlayerIds().size());

        for (long playerId : ctx.getPlayerIds()) {
            Executor.Player.execute(playerId, () -> {
                initPlayerScene(playerId);
            });
        }
    }

    public void createNewPlayer(long playerId, String account) {

        PlayerEntity playerEntity = playerManager.create(playerId, account);
        Player player = new Player(playerEntity);
        initPlayerGame(player);
        int sceneServerId = playerEntity.getSceneServerId();
        playerManager.getSceneServerContextMap().computeIfAbsent(sceneServerId, k -> SceneServerContext.valueOf(sceneServerId)).addPlayer(player.getId());
        initPlayerScene(playerId);
    }

}
