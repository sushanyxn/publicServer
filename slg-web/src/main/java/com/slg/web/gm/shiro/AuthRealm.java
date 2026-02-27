package com.slg.web.gm.shiro;

import com.slg.common.log.LoggerUtil;
import com.slg.web.gm.entity.AdminEntity;
import com.slg.web.gm.service.AdminService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.HashSet;

/**
 * Shiro 认证授权 Realm
 * 从 MySQL（AdminEntity）加载管理员信息进行认证和授权
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class AuthRealm extends AuthorizingRealm {

    @Lazy
    @Autowired
    private AdminService adminService;

    /**
     * 认证：验证用户名和密码
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();
        String password = new String(upToken.getPassword());

        AdminEntity admin = adminService.findByUserName(username);
        if (admin == null) {
            throw new UnknownAccountException("账号不存在: " + username);
        }
        if (!admin.isEnabled()) {
            throw new LockedAccountException("账号已被禁用: " + username);
        }
        if (!ShiroUtils.verifyPassword(password, admin.getSalt(), admin.getPassword())) {
            throw new IncorrectCredentialsException("密码错误");
        }

        LoggerUtil.debug("[Shiro] 管理员认证成功: {}", username);
        return new SimpleAuthenticationInfo(admin, password, getName());
    }

    /**
     * 授权：加载管理员的角色和权限
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        AdminEntity admin = (AdminEntity) principals.getPrimaryPrincipal();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        if (admin.getRoles() != null) {
            info.setRoles(new HashSet<>(admin.getRoles()));
        }
        if (admin.getPermissions() != null) {
            info.setStringPermissions(new HashSet<>(admin.getPermissions()));
        }

        return info;
    }
}
