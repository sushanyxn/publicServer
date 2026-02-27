package com.slg.net.message.clientmessage.scene.packet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场景内军队出现推送。
 * 服务端在场景中某支军队对客户端可见时下发给该客户端。
 *
 * @author yangxunan
 * @date 2026-02-07
 */
@Data
@AllArgsConstructor(staticName = "valueOf")
@NoArgsConstructor
public class SM_SceneArmyAppear {

    /** 军队实体 id（场景内唯一） */
    private long id;

}
