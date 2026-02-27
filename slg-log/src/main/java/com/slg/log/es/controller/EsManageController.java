package com.slg.log.es.controller;

import com.slg.log.es.dto.EsClusterInfo;
import com.slg.log.es.dto.EsIndexInfo;
import com.slg.log.es.service.EsManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * ES 管理接口
 * 提供集群信息查询、索引列表、索引删除等功能
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@RestController
@RequestMapping("/api/es")
@RequiredArgsConstructor
public class EsManageController {

    private final EsManageService esManageService;

    /**
     * 获取集群概览
     */
    @GetMapping("/cluster")
    public EsClusterInfo getClusterInfo() throws IOException {
        return esManageService.getClusterInfo();
    }

    /**
     * 获取索引列表
     */
    @GetMapping("/indices")
    public List<EsIndexInfo> listIndices() throws IOException {
        return esManageService.listIndices();
    }

    /**
     * 删除指定索引（仅管理员）
     */
    @DeleteMapping("/admin/indices/{name}")
    public Map<String, Object> deleteIndex(@PathVariable String name) throws IOException {
        boolean success = esManageService.deleteIndex(name);
        return Map.of("success", success, "indexName", name);
    }
}
