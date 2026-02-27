package com.slg.net.message.clientmessage.report.packet;

import lombok.Data;

/**
 * 战斗兵种详情 VO。
 * 描述单类兵种在战斗中的数量与伤亡（初始、轻伤、重伤、死亡、剩余）。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
public class FightTroopVO {

    /** 兵种配置 id */
    private int troopId;

    /** 战斗开始时兵力 */
    private int initNum;

    /** 轻伤数量 */
    private int hurtNum;

    /** 重伤数量 */
    private int seriousNum;

    /** 死亡数量 */
    private int deadNum;

    /** 剩余数量 */
    private long leastNum;

}
