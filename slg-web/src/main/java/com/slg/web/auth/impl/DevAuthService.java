package com.slg.web.auth.impl;

import com.slg.common.constant.PlatformType;
import com.slg.web.auth.CommonAuthContext;
import com.slg.web.auth.IAuthContext;
import com.slg.web.auth.IAuthService;
import org.springframework.stereotype.Service;

/**
 * 开发模式认证
 * 开发环境下直接以传入的 id 作为用户标识，不做第三方校验
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Service
public class DevAuthService implements IAuthService {

    @Override
    public PlatformType getPlatform() {
        return PlatformType.DEV;
    }

    @Override
    public IAuthContext auth(String id, String token) {
        String userId = (id != null && !id.isEmpty()) ? id : token;
        return CommonAuthContext.success(userId, token);
    }

    @Override
    public IAuthContext bindAuth(String id, String token) {
        String userId = (id != null && !id.isEmpty()) ? id : token;
        return CommonAuthContext.success(userId, token);
    }
}
