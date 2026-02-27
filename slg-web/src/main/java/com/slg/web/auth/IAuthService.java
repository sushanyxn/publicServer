package com.slg.web.auth;

import com.slg.common.constant.PlatformType;

/**
 * 平台认证服务接口
 * 各登录平台需实现此接口提供认证能力
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public interface IAuthService {

    /**
     * 获取平台类型
     */
    PlatformType getPlatform();

    /**
     * 平台登录认证
     *
     * @param id    平台用户标识
     * @param token 平台认证 token
     * @return 认证结果上下文
     */
    IAuthContext auth(String id, String token);

    /**
     * 绑定/解绑时的平台认证
     *
     * @param id    平台用户标识
     * @param token 平台认证 token
     * @return 认证结果上下文
     */
    IAuthContext bindAuth(String id, String token);
}
