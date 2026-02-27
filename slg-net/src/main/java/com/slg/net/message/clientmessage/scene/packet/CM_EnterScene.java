package com.slg.net.message.clientmessage.scene.packet;

import lombok.Getter;
import lombok.Setter;

/**
 * 请求进入场景
 *
 * @author yangxunan
 * @date 2026/2/2
 */
@Getter
@Setter
public class CM_EnterScene {

    /** 场景服 serverId */
    private int serverId;

    /** 场景 id（如主城、世界地图） */
    private int sceneId;

}
