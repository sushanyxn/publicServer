package com.slg.log.search.controller;

import com.slg.log.search.dto.LogSearchRequest;
import com.slg.log.search.dto.LogSearchResponse;
import com.slg.log.search.service.LogSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 日志搜索接口
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogSearchController {

    private final LogSearchService searchService;

    /**
     * 搜索日志
     */
    @PostMapping("/search")
    public ResponseEntity<LogSearchResponse> search(@RequestBody LogSearchRequest request) throws IOException {
        return ResponseEntity.ok(searchService.search(request));
    }

    /**
     * 获取可用的服务器ID列表（供前端筛选下拉框使用）
     */
    @GetMapping("/server-ids")
    public ResponseEntity<List<String>> getServerIds() throws IOException {
        return ResponseEntity.ok(searchService.getServerIds());
    }
}
