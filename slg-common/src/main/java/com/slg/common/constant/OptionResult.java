package com.slg.common.constant;

/**
 * @author yangxunan
 * @date 2026/2/11
 */
public interface OptionResult {

    int SUCCESS = 0;

    // 程序报错 如果是rpc报错，会采用抛异常的方式，不会体现在返回值里
    int UNKNOWN_ERROR = 1;
    // 加载场景玩家时 找不到预存的ScenePlayer
    int SCENE_PLAYER_NOT_FOUND = 2;


}
