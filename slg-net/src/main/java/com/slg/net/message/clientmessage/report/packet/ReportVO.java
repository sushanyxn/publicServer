package com.slg.net.message.clientmessage.report.packet;

import lombok.Data;

import java.util.Map;

/**
 * 战报/报告顶层 VO。
 * 按模块类型（key）聚合各子模块数据，用于客户端展示战斗报告、采集报告等。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
public class ReportVO {

    /** 战报子模块映射，key 为模块类型标识，value 为对应模块 VO */
    private Map<Integer, ReportModuleVO> reportModules;

}
