package com.slg.log.es.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import com.slg.log.es.dto.EsClusterInfo;
import com.slg.log.es.dto.EsIndexInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ES 管理服务
 * 提供集群状态查询、索引列表查询、索引删除等管理功能
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsManageService {

    private final ElasticsearchClient esClient;

    @Value("${elasticsearch.index-prefix}")
    private String indexPrefix;

    /**
     * 获取集群概览信息
     */
    public EsClusterInfo getClusterInfo() throws IOException {
        HealthResponse health = esClient.cluster().health();

        long totalDocs = 0;
        long totalSizeBytes = 0;
        int totalIndices = 0;

        IndicesResponse catResp = esClient.cat().indices(r -> r.index(indexPrefix + "-*"));
        for (IndicesRecord record : catResp.valueBody()) {
            totalIndices++;
            if (record.docsCount() != null) {
                totalDocs += parseLong(record.docsCount());
            }
            if (record.storeSize() != null) {
                totalSizeBytes += parseHumanSize(record.storeSize());
            }
        }

        return new EsClusterInfo(
                health.clusterName(),
                health.status().jsonValue(),
                health.numberOfNodes(),
                totalIndices,
                totalDocs,
                formatBytes(totalSizeBytes)
        );
    }

    /**
     * 获取匹配前缀的所有索引信息列表
     */
    public List<EsIndexInfo> listIndices() throws IOException {
        IndicesResponse catResp = esClient.cat().indices(r -> r.index(indexPrefix + "-*"));
        List<EsIndexInfo> result = new ArrayList<>();

        for (IndicesRecord record : catResp.valueBody()) {
            String name = record.index();
            String status = record.health();
            long docsCount = parseLong(record.docsCount());
            String storeSizeStr = record.storeSize() != null ? record.storeSize() : "0b";
            long storeSizeBytes = parseHumanSize(storeSizeStr);

            result.add(new EsIndexInfo(
                    name,
                    status,
                    docsCount,
                    storeSizeBytes,
                    formatBytes(storeSizeBytes),
                    record.creationDateString() != null ? record.creationDateString() : "-"
            ));
        }

        result.sort(Comparator.comparing(EsIndexInfo::getIndexName).reversed());
        return result;
    }

    /**
     * 删除指定索引
     *
     * @param indexName 索引名称
     * @return 是否成功删除
     */
    public boolean deleteIndex(String indexName) throws IOException {
        if (!indexName.startsWith(indexPrefix)) {
            throw new IllegalArgumentException("只能删除前缀为 " + indexPrefix + " 的索引");
        }
        DeleteIndexResponse resp = esClient.indices().delete(d -> d.index(indexName));
        log.debug("[EsManage] 删除索引: {}, acknowledged={}", indexName, resp.acknowledged());
        return resp.acknowledged();
    }

    /**
     * 将 ES 返回的人可读大小字符串（如 "23.5mb"）解析为字节数
     */
    private long parseHumanSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) {
            return 0;
        }
        sizeStr = sizeStr.trim().toLowerCase();
        try {
            if (sizeStr.endsWith("tb")) {
                return (long) (Double.parseDouble(sizeStr.replace("tb", "")) * 1024L * 1024 * 1024 * 1024);
            } else if (sizeStr.endsWith("gb")) {
                return (long) (Double.parseDouble(sizeStr.replace("gb", "")) * 1024L * 1024 * 1024);
            } else if (sizeStr.endsWith("mb")) {
                return (long) (Double.parseDouble(sizeStr.replace("mb", "")) * 1024L * 1024);
            } else if (sizeStr.endsWith("kb")) {
                return (long) (Double.parseDouble(sizeStr.replace("kb", "")) * 1024);
            } else if (sizeStr.endsWith("b")) {
                return (long) Double.parseDouble(sizeStr.replace("b", ""));
            }
            return Long.parseLong(sizeStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1fMB", bytes / (1024.0 * 1024));
        } else if (bytes < 1024L * 1024 * 1024 * 1024) {
            return String.format("%.2fGB", bytes / (1024.0 * 1024 * 1024));
        } else {
            return String.format("%.2fTB", bytes / (1024.0 * 1024 * 1024 * 1024));
        }
    }
}
