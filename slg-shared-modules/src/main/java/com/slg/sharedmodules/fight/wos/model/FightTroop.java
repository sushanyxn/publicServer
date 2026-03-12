package com.slg.sharedmodules.fight.wos.model;

import com.slg.net.message.clientmessage.report.packet.FightTroopVO;
import lombok.Data;

/**
 * 战斗中单类兵种的参战数据。
 *
 * @author yangxunan
 * @date 2026-02-05
 */
@Data
public class FightTroop {

    /** 兵种配置 id */
    private int troopId;

    /** 当前数量 */
    private int troopNum;

    /** 初始数量 */
    private int troopInitNum;

    /** 受伤/损失数量 */
    private int troopHurtNum;

    /**
     * 构造单类兵种参战数据
     *
     * @param troopId      兵种配置 id
     * @param troopNum     当前数量
     * @param troopInitNum 初始数量
     * @param troopHurtNum 受伤/损失数量
     * @return FightTroop 实例
     */
    public static FightTroop valueOf(int troopId, int troopNum, int troopInitNum, int troopHurtNum) {
        FightTroop t = new FightTroop();
        t.setTroopId(troopId);
        t.setTroopNum(troopNum);
        t.setTroopInitNum(troopInitNum);
        t.setTroopHurtNum(troopHurtNum);
        return t;
    }

    /**
     * 转为战报用兵种详情 VO。
     *
     * @return FightTroopVO，用于战报展示
     */
    public FightTroopVO toFightTroopVO() {
        FightTroopVO vo = new FightTroopVO();
        vo.setTroopId(troopId);
        vo.setInitNum(troopInitNum);
        vo.setHurtNum(troopHurtNum);
        vo.setSeriousNum(0);
        vo.setDeadNum(troopInitNum - troopNum);
        vo.setLeastNum(troopNum);
        return vo;
    }
}
