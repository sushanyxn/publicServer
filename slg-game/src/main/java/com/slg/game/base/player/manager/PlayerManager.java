package com.slg.game.base.player.manager;

import com.slg.common.executor.WorkerThreadPool;
import com.slg.common.log.LoggerUtil;
import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.game.SpringContext;
import com.slg.game.base.player.entity.PlayerEntity;
import com.slg.game.base.player.model.Player;
import com.slg.game.base.player.model.SceneServerContext;
import com.slg.game.core.config.GameServerConfiguration;
import com.slg.redis.cache.accessor.CacheAccessor;
import com.slg.redis.cache.anno.CacheAccessorInject;
import com.slg.redis.cache.entity.PlayerRedisCache;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yangxunan
 * @date 2025/12/23
 */
@Component
@Getter
public class PlayerManager {

    @EntityCacheInject
    private EntityCache<PlayerEntity> playerEntityCache;
    @Autowired
    private GameServerConfiguration gameServerConfiguration;

    @CacheAccessorInject
    private CacheAccessor<PlayerRedisCache> playerCacheAccessor;

    private Map<Long, Player> players = new ConcurrentHashMap<>();
    private Map<Integer, SceneServerContext> sceneServerContextMap = new ConcurrentHashMap<>();

    public void loadPlayers(){

        playerEntityCache.loadAll();

        // 并行加载，并统计场景服分布
        List<Runnable> tasks = new ArrayList<>(playerEntityCache.getAllCache().size());
        AtomicInteger errorCount = new AtomicInteger(0);

        for (PlayerEntity playerEntity : playerEntityCache.getAllCache()) {
            Player player = new Player(playerEntity);

            tasks.add(() -> {
                try {
                    SpringContext.getPlayerService().initPlayerGame(player);
                    int sceneServerId = playerEntity.getSceneServerId();
                    sceneServerContextMap.computeIfAbsent(sceneServerId, k -> SceneServerContext.valueOf(sceneServerId)).addPlayer(player.getId());
                } catch (Exception e) {
                    LoggerUtil.error("玩家{}初始化异常!!", player.getId(), e);
                    errorCount.getAndIncrement();
                }
            });
            WorkerThreadPool.getInstance().executeTasks(tasks);
        }

        if (errorCount.get() > 0) {
            LoggerUtil.error("有{}个玩家初始化异常, 停止启动", errorCount.get());
            throw new RuntimeException("玩家初始化异常!");
        }

        // 根据场景服分布，建立这些场景链接
        // 固定加入自己绑定的场景id
        int bindSceneServerId = gameServerConfiguration.getBindSceneId();
        sceneServerContextMap.putIfAbsent(bindSceneServerId, SceneServerContext.valueOf(bindSceneServerId));

    }

    public void save(PlayerEntity playerEntity){
        playerEntityCache.save(playerEntity);
    }

    public void saveField(PlayerEntity playerEntity, String fieldName){
        playerEntityCache.saveField(playerEntity, fieldName);
    }

    public Collection<PlayerEntity> getAllCache(){
        return playerEntityCache.getAllCache();
    }

    public PlayerEntity create(long playerId, String account){
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.init(playerId, account);
        playerEntityCache.insert(playerEntity);
        return playerEntity;
    }

    public Player getPlayer(long playerId){
        return players.get(playerId);
    }

}
