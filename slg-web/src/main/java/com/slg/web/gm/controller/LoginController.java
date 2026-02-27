package com.slg.web.gm.controller;

import com.slg.common.log.LoggerUtil;
import com.slg.web.response.ErrorCode;
import com.slg.web.response.Response;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * GM 后台登录控制器
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Controller
@RequestMapping("/gm")
public class LoginController {

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String loginPage() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            return "redirect:/gm/console/index";
        }
        return "login";
    }

    /**
     * 执行登录
     */
    @PostMapping("/doLogin")
    @ResponseBody
    public Response<?> doLogin(@RequestParam String username, @RequestParam String password) {
        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);

        try {
            subject.login(token);
            LoggerUtil.debug("[GM] 管理员登录成功: {}", username);
            return Response.success();
        } catch (UnknownAccountException e) {
            return Response.error(ErrorCode.AUTH_FAILED, "账号不存在");
        } catch (LockedAccountException e) {
            return Response.error(ErrorCode.AUTH_FAILED, "账号已被禁用");
        } catch (IncorrectCredentialsException e) {
            return Response.error(ErrorCode.AUTH_FAILED, "密码错误");
        } catch (AuthenticationException e) {
            LoggerUtil.error("[GM] 登录异常: {}", username, e);
            return Response.error(ErrorCode.SYSTEM_ERROR, "登录失败");
        }
    }

    /**
     * 登出
     */
    @GetMapping("/logout")
    public String logout() {
        Subject subject = SecurityUtils.getSubject();
        if (subject.isAuthenticated()) {
            subject.logout();
        }
        return "redirect:/gm/login";
    }

    /**
     * 控制台首页
     */
    @GetMapping("/console/index")
    public String index() {
        return "console/index";
    }

    /**
     * 未授权页面
     */
    @GetMapping("/unauthorized")
    public String unauthorized() {
        return "unauthorized";
    }
}
