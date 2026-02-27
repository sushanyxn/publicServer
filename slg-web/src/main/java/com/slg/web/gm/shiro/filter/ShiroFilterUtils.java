package com.slg.web.gm.shiro.filter;

import com.slg.common.util.JsonUtil;
import com.slg.web.response.ErrorCode;
import com.slg.web.response.Response;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Shiro 过滤器公共工具类
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class ShiroFilterUtils {

    private ShiroFilterUtils() {
    }

    /**
     * 判断请求是否为 Ajax 请求
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        String header = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(header);
    }

    /**
     * 向 Ajax 请求返回 JSON 错误响应
     */
    public static void writeJsonResponse(ServletResponse response, ErrorCode errorCode) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setContentType("application/json;charset=UTF-8");
        httpResponse.setStatus(HttpServletResponse.SC_OK);
        try (PrintWriter writer = httpResponse.getWriter()) {
            writer.write(JsonUtil.toJson(Response.error(errorCode)));
            writer.flush();
        }
    }
}
