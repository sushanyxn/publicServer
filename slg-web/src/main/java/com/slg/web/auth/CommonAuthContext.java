package com.slg.web.auth;

import lombok.Data;

import java.util.Map;

/**
 * 通用认证上下文实现
 * 大部分平台认证结果可直接使用此实现
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Data
public class CommonAuthContext implements IAuthContext {

    private String userId;
    private boolean success;
    private String originalData;
    private Map<String, Object> extData;
    private String userToken;

    /**
     * 创建认证成功的上下文
     */
    public static CommonAuthContext success(String userId, String userToken) {
        CommonAuthContext ctx = new CommonAuthContext();
        ctx.setUserId(userId);
        ctx.setSuccess(true);
        ctx.setUserToken(userToken);
        return ctx;
    }

    /**
     * 创建认证失败的上下文
     */
    public static CommonAuthContext fail(String originalData) {
        CommonAuthContext ctx = new CommonAuthContext();
        ctx.setSuccess(false);
        ctx.setOriginalData(originalData);
        return ctx;
    }
}
