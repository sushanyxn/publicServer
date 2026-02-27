package com.slg.log.stats.controller;

import com.slg.log.stats.dto.LogStatsRequest;
import com.slg.log.stats.dto.LogStatsResponse;
import com.slg.log.stats.service.LogStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 日志统计接口
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class LogStatsController {

    private final LogStatsService statsService;

    /**
     * 获取综合统计数据
     */
    @PostMapping
    public ResponseEntity<LogStatsResponse> getStats(@RequestBody LogStatsRequest request) throws IOException {
        return ResponseEntity.ok(statsService.getStats(request));
    }
}
