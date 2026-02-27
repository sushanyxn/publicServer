package com.slg.scene.net;

import com.slg.scene.base.model.ScenePlayer;

import java.util.Collection;

/**
 * 客户端消息发送工具类
 * 用于向场景玩家发送消息
 *
 * @author yangxunan
 * @date 2026/02/02
 */
public class ToClientPacketUtil {

    /**
     * 向场景玩家发送消息
     *
     * @param playerId 场景玩家
     * @param packet      消息包
     */
    public static void send(long playerId, Object packet) {
    }

    /**
     * 广播发送消息
     * @param playerIds 批量玩家
     * @param packet 消息包
     */
    public static void broadcast(Collection<Long> playerIds, Object packet) {

    }
}
