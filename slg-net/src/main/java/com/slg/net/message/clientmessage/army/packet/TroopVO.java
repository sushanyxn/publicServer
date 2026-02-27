package com.slg.net.message.clientmessage.army.packet;

import lombok.Data;

/**
 * 部队/士兵 VO
 *
 * @author yangxunan
 * @date 2026/2/5
 */
@Data
public class TroopVO {

    /** 兵种配置 id */
    private int troopId;

    /** 当前数量（剩余） */
    private int num;

    /** 初始/出征数量 */
    private int initNum;

    /** 轻伤数量 */
    private int hurtNum;

}
