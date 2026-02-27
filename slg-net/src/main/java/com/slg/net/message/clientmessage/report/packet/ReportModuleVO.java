package com.slg.net.message.clientmessage.report.packet;

import lombok.Data;

/**
 * 战报子模块抽象基类。
 * 各类战报模块（基础信息、英雄、兵种、属性、科技、录像等）继承本类，由 {@link ReportVO} 按类型聚合。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
public abstract class ReportModuleVO {
}
