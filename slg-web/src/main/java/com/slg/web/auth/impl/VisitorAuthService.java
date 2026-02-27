package com.slg.web.auth.impl;

import com.slg.common.constant.PlatformType;
import com.slg.web.auth.CommonAuthContext;
import com.slg.web.auth.IAuthContext;
import com.slg.web.auth.IAuthService;
import org.springframework.stereotype.Service;

/**
 * 游客登录认证
 * 游客模式下直接以设备 ID（token）作为平台用户标识，不做第三方校验
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Service
public class VisitorAuthService implements IAuthService {

    @Override
    public PlatformType getPlatform() {
        return PlatformType.VISITOR;
    }

    @Override
    public IAuthContext auth(String id, String token) {
        return CommonAuthContext.success(token, token);
    }

    @Override
    public IAuthContext bindAuth(String id, String token) {
        return CommonAuthContext.success(token, token);
    }
}
