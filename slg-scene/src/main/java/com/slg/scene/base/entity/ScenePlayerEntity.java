package com.slg.scene.base.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.db.entity.BaseEntity;
import com.slg.net.message.clientmessage.scene.packet.ScenePlayerVO;
import com.slg.net.syncbus.ISyncCache;
import com.slg.net.syncbus.SyncModule;
import com.slg.net.syncbus.anno.SyncEntity;
import com.slg.net.syncbus.anno.SyncField;
import com.slg.scene.SpringContext;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 场景玩家实体
 * 存储玩家在场景服务器中的数据
 *
 * @author yangxunan
 * @date 2026/02/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "scene_player")
@CacheConfig(maxSize = -1, expireMinutes = -1, writeDelaySec = 30)
@FieldNameConstants
@SyncEntity(SyncModule.PLAYER)
public class ScenePlayerEntity extends BaseEntity<Long> implements ISyncCache {

    /**
     * 所属游戏服ID
     */
    private int gameServerId;

    /**
     * 联盟id
     */
    private long allianceId;

    /**
     * 名字
     */
    @SyncField
    private String name;

    /**
     * 初始化场景玩家数据
     *
     * @param scenePlayerVO     vo
     */
    public void init(ScenePlayerVO scenePlayerVO) {
        this.id = scenePlayerVO.getPlayerId();
        this.gameServerId = scenePlayerVO.getGameServerId();
        this.allianceId = scenePlayerVO.getAllianceId();
    }

    /**
     * 全量入库
     */
    @Override
    public void save() {
        SpringContext.getScenePlayerManager().save(this);
    }

    /**
     * 单字段入库
     *
     * @param fieldName 字段名
     */
    @Override
    public void saveField(String fieldName) {
        SpringContext.getScenePlayerManager().saveField(this, fieldName);
    }

    public Long getPlayerId() {
        return id;
    }

    @Override
    public long getSyncId(){
        return id;
    }
}
