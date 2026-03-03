package com.slg.web.account.model;

import lombok.Data;

/**
 * 角色简要信息
 * 存储在 AccountEntity.roleInfoList 中，记录账号下每个角色的核心索引信息
 * 用于登录时快速获取角色所在服，无需查询 UserEntity
 *
 * @author yangxunan
 * @date 2026-03-02
 */
@Data
public class RoleBriefInfo {

    /** 角色 ID */
    private long roleId;

    /** 所在 game 服 ID */
    private int serverId;

    public static RoleBriefInfo of(long roleId, int serverId) {
        RoleBriefInfo info = new RoleBriefInfo();
        info.setRoleId(roleId);
        info.setServerId(serverId);
        return info;
    }
}
