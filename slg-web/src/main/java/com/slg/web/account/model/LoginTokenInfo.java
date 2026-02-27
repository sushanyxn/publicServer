package com.slg.web.account.model;

import lombok.Data;

/**
 * 登录 Token 信息
 * 存储在 Redis 中，供 game 服验证客户端 loginToken 时使用
 * Redis Key 格式：login:token:{tokenValue}
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Data
public class LoginTokenInfo {

    /** 账号 ID */
    private long accountId;

    /** 角色 ID（0 表示新用户，无角色） */
    private long roleId;

    /** 服务器 ID */
    private int serverId;

    /** 平台类型 */
    private int platform;

    /** 是否新注册用户 */
    private boolean register;

    /** 创建时间戳（毫秒） */
    private long createTime;

    public static LoginTokenInfo of(long accountId, long roleId, int serverId, int platform, boolean register) {
        LoginTokenInfo info = new LoginTokenInfo();
        info.setAccountId(accountId);
        info.setRoleId(roleId);
        info.setServerId(serverId);
        info.setPlatform(platform);
        info.setRegister(register);
        info.setCreateTime(System.currentTimeMillis());
        return info;
    }
}
