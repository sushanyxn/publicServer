package com.slg.web.account.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.mysql.entity.BaseMysqlEntity;
import com.slg.web.account.service.UserService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 角色实体（Web 侧）
 * 记录账号在某个 game 服上的角色信息
 * 关系：User(N) → Account(1)，一个账号可在多个服创建角色
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "user_role")
@CacheConfig(maxSize = -1, expireMinutes = -1)
public class UserEntity extends BaseMysqlEntity<Long> {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Override
    public Long getId() {
        return id;
    }

    /** 角色 ID（game 服中的唯一玩家 ID） */
    @Column(name = "role_id")
    private long roleId;

    /** 所在服务器 ID */
    @Column(name = "server_id", nullable = false)
    private long serverId;

    /** 关联的 Account ID */
    @Column(name = "acc_id", nullable = false)
    private long accId;

    /** 封号状态：0=正常，1=封禁 */
    @Column(name = "lock_status")
    private int lockStatus;

    /** 封号结束时间戳（毫秒），0 表示未封 */
    @Column(name = "lock_end_time")
    private long lockEndTime;

    /** 上次登录时间 */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    /** 是否已发送绑定奖励邮件 */
    @Column(name = "send_bind_mail")
    private boolean sendBindMail;

    public static final int LOCK_STATUS_NORMAL = 0;
    public static final int LOCK_STATUS_LOCKED = 1;

    @Override
    public void save() {
        UserService.getInstance().save(this);
    }

    @Override
    public void saveField(String fieldName) {
        UserService.getInstance().saveField(this, fieldName);
    }
}
