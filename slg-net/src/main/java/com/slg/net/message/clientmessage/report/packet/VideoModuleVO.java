package com.slg.net.message.clientmessage.report.packet;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 战报录像模块 VO。
 * 描述本场战报关联的录像 id，用于客户端拉取或播放战斗回放。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VideoModuleVO extends ReportModuleVO {

    /** 录像 id，用于拉取/播放回放 */
    private long videoId;

}
