package com.slg.scene.net.resolver;

import com.slg.net.syncbus.ISyncCacheResolver;
import com.slg.net.syncbus.SyncModule;
import com.slg.scene.SpringContext;
import com.slg.scene.base.entity.ScenePlayerEntity;
import com.slg.scene.base.model.ScenePlayer;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/2/12
 */
@Component
public class ScenePlayerSyncResolver implements ISyncCacheResolver<ScenePlayerEntity> {
    @Override
    public SyncModule getSyncModule(){
        return SyncModule.PLAYER;
    }

    @Override
    public ScenePlayerEntity resolve(long syncId){
        ScenePlayer scenePlayer = SpringContext.getScenePlayerManager().getScenePlayer(syncId);
        if (scenePlayer != null) {
            return scenePlayer.getScenePlayerEntity();
        }
        return null;
    }
}
