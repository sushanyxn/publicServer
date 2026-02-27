package com.slg.log.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.slg.log.search.dto.LogSearchRequest;
import com.slg.log.search.dto.LogSearchResponse;
import com.slg.log.search.dto.LogSearchResponse.LogEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * 日志搜索服务
 * 解析搜索语法并转换为 ES BoolQuery
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogSearchService {

    private final ElasticsearchClient esClient;

    @Value("${elasticsearch.index-prefix}")
    private String indexPrefix;

    /**
     * 搜索日志
     * <p>
     * 搜索语法：多个关键词用空格分隔
     * - 普通词：必须包含（AND 关系）
     * - ~前缀词：排除（NOT 关系）
     * <p>
     * 示例：NullPointerException PlayerManager ~heartbeat ~ping
     */
    public LogSearchResponse search(LogSearchRequest request) throws IOException {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        if (StringUtils.hasText(request.getQuery())) {
            parseQueryTerms(request.getQuery(), boolBuilder);
        }

        addFilters(request, boolBuilder);

        SearchResponse<ObjectNode> response = esClient.search(s -> s
                        .index(indexPrefix + "-*")
                        .query(q -> q.bool(boolBuilder.build()))
                        .from(request.getPage() * request.getSize())
                        .size(request.getSize())
                        .sort(sort -> sort.field(f -> f.field("@timestamp").order(SortOrder.Desc)))
                        .highlight(h -> h
                                .fields("message", hf -> hf
                                        .preTags("<em>")
                                        .postTags("</em>")
                                        .fragmentSize(200)
                                        .numberOfFragments(3))
                                .fields("stack_trace", hf -> hf
                                        .preTags("<em>")
                                        .postTags("</em>")
                                        .fragmentSize(300)
                                        .numberOfFragments(2))),
                ObjectNode.class);

        List<LogEntry> entries = new ArrayList<>();
        for (Hit<ObjectNode> hit : response.hits().hits()) {
            ObjectNode source = hit.source();
            if (source == null) {
                continue;
            }
            LogEntry entry = new LogEntry();
            entry.setTimestamp(getTextField(source, "@timestamp"));
            entry.setLevel(getTextField(source, "level"));
            entry.setLoggerName(getTextField(source, "logger_name"));
            entry.setThreadName(getTextField(source, "thread_name"));
            entry.setMessage(getTextField(source, "message"));
            entry.setStackTrace(getTextField(source, "stack_trace"));
            entry.setServerId(getTextField(source, "server_id"));
            entry.setServerType(getTextField(source, "server_type"));

            if (hit.highlight() != null && !hit.highlight().isEmpty()) {
                entry.setHighlights(hit.highlight());
            }
            entries.add(entry);
        }

        long total = response.hits().total() != null ? response.hits().total().value() : 0;
        return new LogSearchResponse(total, entries);
    }

    /**
     * 获取可用的服务器列表（从 ES 聚合中提取）
     */
    public List<String> getServerIds() throws IOException {
        SearchResponse<Void> response = esClient.search(s -> s
                        .index(indexPrefix + "-*")
                        .size(0)
                        .aggregations("server_ids", a -> a
                                .terms(t -> t.field("server_id").size(200))),
                Void.class);

        List<String> serverIds = new ArrayList<>();
        var buckets = response.aggregations().get("server_ids").sterms().buckets().array();
        for (var bucket : buckets) {
            serverIds.add(bucket.key().stringValue());
        }
        return serverIds;
    }

    /**
     * 解析搜索词，对 message 使用 match（分词匹配），
     * 对 stack_trace 使用 wildcard（兼容 keyword/wildcard 类型字段），
     * 两者 should 组合，任一命中即匹配
     */
    private void parseQueryTerms(String query, BoolQuery.Builder boolBuilder) {
        String[] terms = query.trim().split("\\s+");
        for (String term : terms) {
            if (term.isEmpty()) {
                continue;
            }
            if (term.startsWith("~") && term.length() > 1) {
                String excluded = term.substring(1);
                boolBuilder.mustNot(q -> q.bool(b -> b
                        .should(s -> s.match(m -> m.field("message").query(excluded)))
                        .should(s -> s.wildcard(w -> w
                                .field("stack_trace")
                                .value("*" + excluded + "*")
                                .caseInsensitive(true)))));
            } else {
                boolBuilder.must(q -> q.bool(b -> b
                        .should(s -> s.match(m -> m.field("message").query(term)))
                        .should(s -> s.wildcard(w -> w
                                .field("stack_trace")
                                .value("*" + term + "*")
                                .caseInsensitive(true)))));
            }
        }
    }

    private void addFilters(LogSearchRequest request, BoolQuery.Builder boolBuilder) {
        if (StringUtils.hasText(request.getStartTime()) || StringUtils.hasText(request.getEndTime())) {
            boolBuilder.filter(f -> f.range(r -> r.date(d -> {
                d.field("@timestamp");
                if (StringUtils.hasText(request.getStartTime())) {
                    d.gte(request.getStartTime());
                }
                if (StringUtils.hasText(request.getEndTime())) {
                    d.lte(request.getEndTime());
                }
                return d;
            })));
        }

        if (StringUtils.hasText(request.getServerId())) {
            boolBuilder.filter(f -> f.term(t -> t.field("server_id").value(request.getServerId())));
        }

        if (StringUtils.hasText(request.getServerType())) {
            boolBuilder.filter(f -> f.term(t -> t.field("server_type").value(request.getServerType())));
        }

        if (StringUtils.hasText(request.getLevel())) {
            boolBuilder.filter(f -> f.term(t -> t.field("level").value(request.getLevel())));
        }
    }

    private String getTextField(ObjectNode node, String field) {
        var jsonNode = node.get(field);
        return jsonNode != null && !jsonNode.isNull() ? jsonNode.asText() : null;
    }
}
