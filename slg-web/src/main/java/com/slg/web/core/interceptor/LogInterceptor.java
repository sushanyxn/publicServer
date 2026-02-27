package com.slg.web.core.interceptor;

import com.slg.common.log.LoggerUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求日志拦截器
 * 记录 HTTP 请求的方法、URI 和耗时
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class LogInterceptor implements HandlerInterceptor {

    private static final String ATTR_START_TIME = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(ATTR_START_TIME);
        if (startTime != null) {
            long elapsed = System.currentTimeMillis() - startTime;
            LoggerUtil.debug("[HTTP] {} {} -> {} ({}ms)",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), elapsed);
        }
    }
}
