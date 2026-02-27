package com.slg.log.es.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ES 集群信息 DTO
 * 用于前端展示集群概览状态
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsClusterInfo {

    /** 集群名称 */
    private String clusterName;

    /** 集群状态（green/yellow/red） */
    private String status;

    /** 节点数量 */
    private int numberOfNodes;

    /** 索引总数 */
    private int totalIndices;

    /** 文档总数 */
    private long totalDocs;

    /** 总存储大小（可读格式） */
    private String totalSizeHuman;
}
