package com.slg.game.base.player.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.db.entity.BaseEntity;
import com.slg.game.SpringContext;
import com.slg.game.develop.hero.model.HeroPlayerInfo;
import com.slg.game.develop.task.model.MainTaskInfo;
import com.slg.net.syncbus.ISyncHolder;
import com.slg.net.syncbus.SyncModule;
import com.slg.net.syncbus.anno.SyncEntity;
import com.slg.net.syncbus.anno.SyncField;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yangxunan
 * @date 2025/12/23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "player")
@CacheConfig(maxSize = -1, expireMinutes = -1, writeDelaySec = 30)
@FieldNameConstants
@SyncEntity(SyncModule.PLAYER)
public class PlayerEntity extends BaseEntity<Long> implements ISyncHolder {

    /**
     * 账号
     */
    private String account;

    /**
     * 名字
     */
    @SyncField(syncInterval = 2)
    private String name;

    /**
     * 服务器id
     */
    private int serverId;

    /**
     * 场景服id
     */
    private int sceneServerId;

    /**
     * 英雄信息
     */
    private HeroPlayerInfo heroPlayerInfo = new HeroPlayerInfo();

    /**
     * 主线任务
     */
    private MainTaskInfo mainTaskInfo = new MainTaskInfo();


    public void init(long playerId, String account){
        this.id = playerId;
        this.serverId = SpringContext.getGameserverConfiguration().getServerId();
        this.sceneServerId = SpringContext.getGameserverConfiguration().getBindSceneId();
    }

    /**
     * 全量入库
     */
    @Override
    public void save(){
        SpringContext.getPlayerManager().save(this);
    }

    /**
     * 单字段入库
     * @param fieldName
     */
    @Override
    public void saveField(String fieldName){
        SpringContext.getPlayerManager().saveField(this, fieldName);
    }


    @Override
    public long getSyncId(){
        return id;
    }

    @Override
    public int[] syncTargetServerIds(){
        return new int[]{sceneServerId};
    }
}
