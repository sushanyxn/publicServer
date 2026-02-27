package com.slg.web.auth;

import com.slg.common.constant.PlatformType;
import com.slg.common.log.LoggerUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 认证服务注册中心
 * 启动时自动扫描所有 IAuthService 实现，按平台类型映射
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Service
public class AuthRepository {

    @Autowired
    private ApplicationContext appCtx;

    private Map<PlatformType, IAuthService> authMap = new EnumMap<>(PlatformType.class);

    @PostConstruct
    public void init() {
        Map<String, IAuthService> temp = appCtx.getBeansOfType(IAuthService.class, false, true);
        for (IAuthService service : temp.values()) {
            authMap.put(service.getPlatform(), service);
            LoggerUtil.debug("[Auth] 注册认证服务: {} -> {}", service.getPlatform(), service.getClass().getSimpleName());
        }
        this.authMap = Collections.unmodifiableMap(authMap);
        LoggerUtil.debug("[Auth] 认证服务注册完成，共 {} 个平台", authMap.size());
    }

    /**
     * 根据平台 ID 获取认证服务
     *
     * @return 认证服务，不支持的平台返回 null
     */
    public IAuthService getService(int platform) {
        PlatformType type = PlatformType.findById(platform);
        return getService(type);
    }

    /**
     * 根据平台类型获取认证服务
     */
    public IAuthService getService(PlatformType platform) {
        return this.authMap.get(platform);
    }
}
