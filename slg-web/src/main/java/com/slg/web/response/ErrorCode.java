package com.slg.web.response;

import lombok.Getter;

/**
 * 统一错误码定义
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    PARAM_ERROR(1001, "参数错误"),
    AUTH_FAILED(1002, "认证失败"),
    PLATFORM_NOT_SUPPORTED(1003, "不支持的平台"),
    SERVER_NOT_AVAILABLE(1004, "服务器不可用"),
    ACCOUNT_BANNED(1005, "账号已被封禁"),
    TOKEN_EXPIRED(1006, "Token 已过期"),
    SERVER_MAINTENANCE(1007, "服务器维护中"),
    VERSION_TOO_LOW(1008, "版本过低，请更新"),
    ACCOUNT_NOT_FOUND(1009, "账号不存在"),
    USER_NOT_FOUND(1010, "角色不存在"),
    RECOMMEND_SERVER_NOT_FOUND(1011, "未找到推荐服务器"),
    REPEAT_REQUEST(1012, "重复请求"),
    SYSTEM_ERROR(9999, "系统错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
