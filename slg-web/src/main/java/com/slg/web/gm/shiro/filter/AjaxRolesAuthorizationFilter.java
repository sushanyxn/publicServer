package com.slg.web.gm.shiro.filter;

import com.slg.web.response.ErrorCode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.shiro.web.filter.authz.RolesAuthorizationFilter;

import java.io.IOException;

/**
 * Ajax 角色授权过滤器
 * 对 Ajax 请求返回 JSON 而非跳转未授权页
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class AjaxRolesAuthorizationFilter extends RolesAuthorizationFilter {

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException {
        if (ShiroFilterUtils.isAjaxRequest((HttpServletRequest) request)) {
            ShiroFilterUtils.writeJsonResponse(response, ErrorCode.AUTH_FAILED);
            return false;
        }
        return super.onAccessDenied(request, response);
    }
}
