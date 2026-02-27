package com.slg.redis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Redis 排行榜服务
 * 基于 Sorted Set 实现排行榜功能
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Component
public class RedisRankingService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加或更新排行榜成员分数
     *
     * @param rankKey 排行榜键
     * @param member  成员标识
     * @param score   分数
     * @return true 新增成功，false 更新已有成员
     */
    public Boolean addScore(String rankKey, String member, double score) {
        return stringRedisTemplate.opsForZSet().add(rankKey, member, score);
    }

    /**
     * 增量更新成员分数
     *
     * @param rankKey 排行榜键
     * @param member  成员标识
     * @param delta   分数增量
     * @return 更新后的分数
     */
    public Double incrementScore(String rankKey, String member, double delta) {
        return stringRedisTemplate.opsForZSet().incrementScore(rankKey, member, delta);
    }

    /**
     * 获取成员排名（从高到低，0 为第一名）
     *
     * @param rankKey 排行榜键
     * @param member  成员标识
     * @return 排名（0-based），成员不存在返回 null
     */
    public Long getRank(String rankKey, String member) {
        return stringRedisTemplate.opsForZSet().reverseRank(rankKey, member);
    }

    /**
     * 获取成员分数
     *
     * @param rankKey 排行榜键
     * @param member  成员标识
     * @return 分数，成员不存在返回 null
     */
    public Double getScore(String rankKey, String member) {
        return stringRedisTemplate.opsForZSet().score(rankKey, member);
    }

    /**
     * 获取排行榜前 N 名（从高到低）
     *
     * @param rankKey 排行榜键
     * @param n       前 N 名
     * @return 成员集合（按分数从高到低），空排行榜返回空集合
     */
    public Set<String> getTopN(String rankKey, long n) {
        Set<String> result = stringRedisTemplate.opsForZSet().reverseRange(rankKey, 0, n - 1);
        return result != null ? result : Collections.emptySet();
    }

    /**
     * 获取排行榜指定范围的成员及分数（从高到低）
     *
     * @param rankKey 排行榜键
     * @param start   起始排名（0-based）
     * @param end     结束排名（包含）
     * @return 成员与分数的有序映射，按分数从高到低排列
     */
    public Map<String, Double> getRangeWithScores(String rankKey, long start, long end) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(rankKey, start, end);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Double> result = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            result.put(tuple.getValue(), tuple.getScore());
        }
        return result;
    }

    /**
     * 获取指定分数区间的成员及分数（从高到低）
     *
     * @param rankKey  排行榜键
     * @param minScore 最低分数
     * @param maxScore 最高分数
     * @return 成员与分数的有序映射
     */
    public Map<String, Double> getByScoreRange(String rankKey, double minScore, double maxScore) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(rankKey, minScore, maxScore);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Double> result = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            result.put(tuple.getValue(), tuple.getScore());
        }
        return result;
    }

    /**
     * 移除排行榜成员
     *
     * @param rankKey 排行榜键
     * @param members 要移除的成员
     * @return 移除的数量
     */
    public Long removeMembers(String rankKey, String... members) {
        return stringRedisTemplate.opsForZSet().remove(rankKey, (Object[]) members);
    }

    /**
     * 获取排行榜总人数
     *
     * @param rankKey 排行榜键
     * @return 排行榜中的成员总数
     */
    public Long getRankSize(String rankKey) {
        Long size = stringRedisTemplate.opsForZSet().zCard(rankKey);
        return size != null ? size : 0L;
    }

    /**
     * 删除整个排行榜
     *
     * @param rankKey 排行榜键
     * @return true 删除成功
     */
    public Boolean deleteRank(String rankKey) {
        return stringRedisTemplate.delete(rankKey);
    }
}
