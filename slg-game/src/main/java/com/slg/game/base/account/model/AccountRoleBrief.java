package com.slg.game.base.account.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账号下角色简要信息
 * 描述某账号下某角色在账号维度的简要数据（如最近登录时间等），后续可扩展更多字段
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Data
public class AccountRoleBrief {

    /** 角色 ID */
    private long roleId;

    /** 该角色最近一次登录时间 */
    private LocalDateTime lastLoginTime;

    /**
     * 构建一条角色简要信息
     *
     * @param roleId        角色 ID
     * @param lastLoginTime 最近登录时间
     * @return 简要信息实例
     */
    public static AccountRoleBrief valueOf(long roleId, LocalDateTime lastLoginTime) {
        AccountRoleBrief brief = new AccountRoleBrief();
        brief.setRoleId(roleId);
        brief.setLastLoginTime(lastLoginTime);
        return brief;
    }
}
