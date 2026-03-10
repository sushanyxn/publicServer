package com.slg.common.hotreload;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 热更操作的整体结果
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Getter
public class HotReloadResult {

    private final String dirPath;
    private final int totalFiles;
    private final int successCount;
    private final int failCount;
    private final long costMs;
    private final List<ClassReloadDetail> details;
    private final String errorMessage;

    private HotReloadResult(String dirPath, List<ClassReloadDetail> details, long costMs, String errorMessage) {
        this.dirPath = dirPath;
        this.details = Collections.unmodifiableList(details);
        this.costMs = costMs;
        this.errorMessage = errorMessage;

        this.totalFiles = details.size();
        this.successCount = (int) details.stream().filter(ClassReloadDetail::isSuccess).count();
        this.failCount = totalFiles - successCount;
    }

    /**
     * 正常完成（可能部分成功部分失败）
     */
    public static HotReloadResult valueOf(String dirPath, List<ClassReloadDetail> details, long costMs) {
        return new HotReloadResult(dirPath, details, costMs, null);
    }

    /**
     * 前置校验失败，未执行任何热更
     */
    public static HotReloadResult error(String dirPath, String errorMessage) {
        return new HotReloadResult(dirPath, Collections.emptyList(), 0, errorMessage);
    }

    public boolean isAllSuccess() {
        return errorMessage == null && failCount == 0 && totalFiles > 0;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public String getSummary() {
        if (hasError()) {
            return String.format("热更失败 [%s]: %s", dirPath, errorMessage);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("热更完成 [%s]: 共 %d 个类, 成功 %d, 失败 %d, 耗时 %dms",
                dirPath, totalFiles, successCount, failCount, costMs));

        List<ClassReloadDetail> failures = new ArrayList<>();
        for (ClassReloadDetail detail : details) {
            if (!detail.isSuccess()) {
                failures.add(detail);
            }
        }
        if (!failures.isEmpty()) {
            sb.append("\n失败详情:");
            for (ClassReloadDetail fail : failures) {
                sb.append("\n  ").append(fail);
            }
        }
        return sb.toString();
    }
}
