package com.slg.common.hotreload;

import lombok.Getter;

/**
 * 单个类的热更详情
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Getter
public class ClassReloadDetail {

    private final String className;
    private final ReloadStatus status;
    private final String message;

    private ClassReloadDetail(String className, ReloadStatus status, String message) {
        this.className = className;
        this.status = status;
        this.message = message;
    }

    public static ClassReloadDetail valueOf(String className, ReloadStatus status, String message) {
        return new ClassReloadDetail(className, status, message);
    }

    public static ClassReloadDetail success(String className, ReloadStatus status) {
        return new ClassReloadDetail(className, status, null);
    }

    public boolean isSuccess() {
        return status.isSuccess();
    }

    @Override
    public String toString() {
        String statusStr = status.isSuccess() ? "OK" : "FAIL";
        return String.format("[%s] %s%s", statusStr, className, message != null ? " - " + message : "");
    }
}
