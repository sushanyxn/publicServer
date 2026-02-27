package com.slg.net.message.clientmessage.scene.packet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场景内军队消失推送。
 * 服务端在场景中某支军队对客户端不再可见时下发给该客户端。
 *
 * @author yangxunan
 * @date 2026-02-07
 */
@Data
@AllArgsConstructor(staticName = "valueOf")
@NoArgsConstructor
public class SM_SceneArmyDisappear {

    /** 军队实体 id（场景内唯一） */
    private long id;

}
