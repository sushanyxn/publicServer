package com.slg.scene.base.manager;

import com.slg.common.log.LoggerUtil;
import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.net.message.clientmessage.scene.packet.ScenePlayerVO;
import com.slg.scene.base.entity.ScenePlayerEntity;
import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.base.model.WatchPlayer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 场景玩家管理器
 * 负责管理场景玩家数据和实例
 *
 * @author yangxunan
 * @date 2026/1/23
 */
@Component
@Getter
public class ScenePlayerManager {

    @EntityCacheInject
    private EntityCache<ScenePlayerEntity> scenePlayerEntityCache;

    /**
     * 场景玩家映射
     * key: 玩家ID
     * value: 场景玩家对象
     */
    private Map<Long, ScenePlayer> scenePlayers = new ConcurrentHashMap<>();

    /**
     * 观察者缓存
     * 没有实体
     */
    private Map<Long, WatchPlayer> watchPlayers = new ConcurrentHashMap<>();

    /**
     * 加载所有场景玩家数据
     */
    public void loadScenePlayers(){
        LoggerUtil.debug("开始加载场景玩家数据");

        scenePlayerEntityCache.loadAll();

        for (ScenePlayerEntity scenePlayerEntity : scenePlayerEntityCache.getAllCache()) {
            ScenePlayer scenePlayer = new ScenePlayer(scenePlayerEntity);
            try {
                // TODO: 初始化场景玩家相关数据
                scenePlayers.put(scenePlayerEntity.getPlayerId(), scenePlayer);
            } catch (Exception e) {
                LoggerUtil.error("场景玩家{}初始化异常!!", scenePlayer.getId(), e);
            }
        }

        LoggerUtil.debug("场景玩家数据加载完成，共加载{}个玩家", scenePlayers.size());
    }

    /**
     * 保存场景玩家实体
     *
     * @param scenePlayerEntity 场景玩家实体
     */
    public void save(ScenePlayerEntity scenePlayerEntity){
        scenePlayerEntityCache.save(scenePlayerEntity);
    }

    /**
     * 保存场景玩家实体的指定字段
     *
     * @param scenePlayerEntity 场景玩家实体
     * @param fieldName         字段名
     */
    public void saveField(ScenePlayerEntity scenePlayerEntity, String fieldName){
        scenePlayerEntityCache.saveField(scenePlayerEntity, fieldName);
    }

    /**
     * 获取所有缓存的场景玩家实体
     *
     * @return 场景玩家实体集合
     */
    public Collection<ScenePlayerEntity> getAllCache(){
        return scenePlayerEntityCache.getAllCache();
    }

    /**
     * 创建新的场景玩家
     *
     * @param scenePlayerVO 玩家vo
     * @return 场景玩家实体
     */
    public ScenePlayerEntity create(ScenePlayerVO scenePlayerVO){

        ScenePlayerEntity scenePlayerEntity = new ScenePlayerEntity();
        scenePlayerEntity.init(scenePlayerVO);
        scenePlayerEntityCache.insert(scenePlayerEntity);

        // 创建场景玩家对象
        ScenePlayer scenePlayer = new ScenePlayer(scenePlayerEntity);
        scenePlayers.put(scenePlayer.getId(), scenePlayer);

        // 从watchPlayer中移除
        watchPlayers.remove(scenePlayerEntity.getPlayerId());

        return scenePlayerEntity;
    }

    /**
     * 获取指定 GameServer 下所有已初始化的场景玩家
     * 通过 ScenePlayerEntity 中的 gameServerId 字段过滤
     * 用于 Game 断线时批量清理
     *
     * @param gameServerId 游戏服务器ID
     * @return 已初始化的场景玩家列表
     */
    public List<ScenePlayer> getInitedPlayersByGameServer(int gameServerId) {
        return scenePlayers.values().stream()
                .filter(sp -> sp.getScenePlayerEntity().getGameServerId() == gameServerId && sp.isSceneInited())
                .toList();
    }

    /**
     * 获取场景玩家
     *
     * @param playerId 玩家ID
     * @return 场景玩家
     */
    public ScenePlayer getScenePlayer(long playerId){
        ScenePlayer scenePlayer = scenePlayers.get(playerId);
        if (scenePlayer == null) {
            scenePlayer = watchPlayers.get(playerId);
        }
        return scenePlayer;
    }

    /**
     * 创建临时的watchPlayer
     * @param playerId
     * @return
     */
    public WatchPlayer createTempWatchPlayer(long playerId){

        return watchPlayers.computeIfAbsent(playerId, WatchPlayer::new);

    }

}

