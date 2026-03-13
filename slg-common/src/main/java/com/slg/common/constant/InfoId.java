package com.slg.common.constant;

/**
 * 提示信息 ID 常量
 * 每个常量对应 MessageTable.csv 中的一条消息记录，客户端通过 ID 查找显示内容
 *
 * @author yangxunan
 * @date 2026/03/13
 */
public interface InfoId {

    /** 英雄不存在 */
    int HERO_NOT_FOUND = 1001;
    /** 英雄已达最大等级 */
    int HERO_MAX_LEVEL = 1002;

}
