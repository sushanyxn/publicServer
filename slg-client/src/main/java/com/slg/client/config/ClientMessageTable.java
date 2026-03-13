package com.slg.client.config;

import com.slg.table.anno.Table;
import com.slg.table.anno.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * 客户端消息配置表
 * 读取 MessageTable.csv，用于将服务端下发的 infoId 映射为可读的提示内容
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Table(alias = "MessageTable")
@Getter
@Setter
public class ClientMessageTable {

    @TableId
    private int id;

    /** 消息中文内容 */
    private String content;

}
