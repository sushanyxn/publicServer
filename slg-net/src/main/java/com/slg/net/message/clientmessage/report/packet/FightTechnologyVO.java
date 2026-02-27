package com.slg.net.message.clientmessage.report.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 战报科技数据 VO。
 * 描述战斗中生效的科技研究进度（科技 id -> 研究百分比）。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
public class FightTechnologyVO {

    /** 科技 id -> 研究百分比（如 0-100） */
    private Map<Integer, Integer> techRate;

}
