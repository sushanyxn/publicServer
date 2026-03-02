package com.slg.web.net.rpc.service;

import com.slg.common.log.LoggerUtil;
import com.slg.net.rpc.impl.web.IWebRpcService;
import com.slg.web.account.entity.AccountEntity;
import com.slg.web.account.entity.UserEntity;
import com.slg.web.account.service.AccountService;
import com.slg.web.account.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Web 服 RPC 接口实现
 * 处理 game 侧发来的创角、登出等回调，维护 UserEntity 与 Account.roleInfoList
 *
 * @author yangxunan
 * @date 2026-03-02
 */
@Component
public class WebRpcServiceImpl implements IWebRpcService {

    @Autowired
    private AccountService accountService;
    @Autowired
    private UserService userService;

    /**
     * 创角回调（幂等）
     * 若 UserEntity 已存在则仅更新 serverId；Account.roleInfoList 同步更新
     * Account.mainRoleId 在首次创角时自动设置
     */
    @Override
    public void onRoleCreated(int webServerId, long accountId, long roleId, long gameServerId) {
        AccountEntity account = accountService.findById(accountId);
        if (account == null) {
            LoggerUtil.error("[WebRpc] 创角回调失败：accountId={} 不存在, roleId={}", accountId, roleId);
            return;
        }

        UserEntity user = userService.findByRoleId(roleId);
        if (user == null) {
            user = userService.createUser(roleId, gameServerId, accountId);
            LoggerUtil.info("[WebRpc] 创角成功: accountId={}, roleId={}, serverId={}", accountId, roleId, gameServerId);
        } else {
            // 幂等：角色已存在，仅更新所在服（迁服场景）
            if (user.getServerId() != gameServerId) {
                user.setServerId(gameServerId);
                userService.save(user);
            }
        }

        // 更新 Account.roleInfoList
        accountService.addRoleInfo(account, roleId, gameServerId);

        // 首次创角时设置 mainRoleId
        if (account.getMainRoleId() == 0) {
            account.setMainRoleId(roleId);
            accountService.save(account);
        }
    }

    /**
     * 登出回调
     * 更新 UserEntity.lastLoginTime（此处记录的是登录时间，登出时一并确认落库）
     */
    @Override
    public void onRoleLogout(int webServerId, long roleId, long lastLoginTime) {
        UserEntity user = userService.findByRoleId(roleId);
        if (user == null) {
            LoggerUtil.error("[WebRpc] 登出回调失败：roleId={} 对应的 UserEntity 不存在", roleId);
            return;
        }
        LocalDateTime loginTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(lastLoginTime), ZoneId.systemDefault());
        user.setLastLoginTime(loginTime);
        userService.save(user);
        LoggerUtil.info("[WebRpc] 角色登出记录: roleId={}, lastLoginTime={}", roleId, loginTime);
    }
}
