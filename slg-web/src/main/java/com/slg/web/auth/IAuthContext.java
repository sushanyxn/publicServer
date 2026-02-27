package com.slg.web.auth;

import java.util.Map;

/**
 * 平台认证结果上下文
 * 封装认证后的用户信息和状态
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public interface IAuthContext {

    /**
     * 认证后的平台用户 ID
     */
    String getUserId();

    /**
     * 认证是否成功
     */
    boolean isSuccess();

    /**
     * 平台返回的原始数据（认证失败时可直接返回给客户端）
     */
    String getOriginalData();

    /**
     * 获取扩展数据（返回给客户端的附加信息）
     */
    Map<String, Object> getExtData();

    /**
     * 获取平台用户 token（可回传给客户端用于后续调用）
     */
    String getUserToken();
}
