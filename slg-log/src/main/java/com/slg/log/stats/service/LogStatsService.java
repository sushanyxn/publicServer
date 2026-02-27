package com.slg.log.stats.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;

import com.slg.log.stats.dto.LogStatsRequest;
import com.slg.log.stats.dto.LogStatsResponse;
import com.slg.log.stats.dto.LogStatsResponse.BucketCount;
import com.slg.log.stats.dto.LogStatsResponse.DayServerCount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志统计聚合服务
 * 利用 ES 的 Aggregation 实现多维度统计
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogStatsService {

    private final ElasticsearchClient esClient;

    @Value("${elasticsearch.index-prefix}")
    private String indexPrefix;

    /**
     * 综合统计：按天、按服务器、按级别、交叉统计
     */
    public LogStatsResponse getStats(LogStatsRequest request) throws IOException {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        addFilters(request, boolBuilder);

        SearchResponse<Void> response = esClient.search(s -> s
                .index(indexPrefix + "-*")
                .size(0)
                .query(q -> q.bool(boolBuilder.build()))
                .aggregations("by_day", a -> a
                        .dateHistogram(d -> d
                                .field("@timestamp")
                                .calendarInterval(CalendarInterval.Day)
                                .format("yyyy-MM-dd"))
                        .aggregations("by_server", sub -> sub
                                .terms(t -> t.field("server_id").size(200))))
                .aggregations("by_server", a -> a
                        .terms(t -> t.field("server_id").size(200)))
                .aggregations("by_level", a -> a
                        .terms(t -> t.field("level").size(10))),
                Void.class);

        LogStatsResponse stats = new LogStatsResponse();
        stats.setTotalCount(response.hits().total() != null ? response.hits().total().value() : 0);
        stats.setByDay(extractDayBuckets(response));
        stats.setByServer(extractTermBuckets(response, "by_server"));
        stats.setByLevel(extractTermBuckets(response, "by_level"));
        stats.setByDayAndServer(extractDayServerBuckets(response));

        return stats;
    }

    private List<BucketCount> extractDayBuckets(SearchResponse<Void> response) {
        List<BucketCount> result = new ArrayList<>();
        var agg = response.aggregations().get("by_day");
        if (agg != null) {
            for (var bucket : agg.dateHistogram().buckets().array()) {
                result.add(new BucketCount(bucket.keyAsString(), bucket.docCount()));
            }
        }
        return result;
    }

    private List<BucketCount> extractTermBuckets(SearchResponse<Void> response, String aggName) {
        List<BucketCount> result = new ArrayList<>();
        var agg = response.aggregations().get(aggName);
        if (agg != null) {
            for (var bucket : agg.sterms().buckets().array()) {
                result.add(new BucketCount(bucket.key().stringValue(), bucket.docCount()));
            }
        }
        return result;
    }

    private List<DayServerCount> extractDayServerBuckets(SearchResponse<Void> response) {
        List<DayServerCount> result = new ArrayList<>();
        var dayAgg = response.aggregations().get("by_day");
        if (dayAgg != null) {
            for (var dayBucket : dayAgg.dateHistogram().buckets().array()) {
                String day = dayBucket.keyAsString();
                var serverAgg = dayBucket.aggregations().get("by_server");
                if (serverAgg != null) {
                    for (var serverBucket : serverAgg.sterms().buckets().array()) {
                        result.add(new DayServerCount(day, serverBucket.key().stringValue(), serverBucket.docCount()));
                    }
                }
            }
        }
        return result;
    }

    private void addFilters(LogStatsRequest request, BoolQuery.Builder boolBuilder) {
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

        if (StringUtils.hasText(request.getLevel())) {
            boolBuilder.filter(f -> f.term(t -> t.field("level").value(request.getLevel())));
        }
    }
}
