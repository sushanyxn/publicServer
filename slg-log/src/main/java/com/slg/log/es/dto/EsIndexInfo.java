package com.slg.log.es.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ES 索引信息 DTO
 * 用于前端展示索引的基本状态和存储信息
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsIndexInfo {

    /** 索引名称 */
    private String indexName;

    /** 索引状态（green/yellow/red） */
    private String status;

    /** 文档数量 */
    private long docsCount;

    /** 存储大小（字节） */
    private long storeSizeBytes;

    /** 存储大小（可读格式，如 "23.5MB"） */
    private String storeSizeHuman;

    /** 索引创建时间 */
    private String creationDate;
}
