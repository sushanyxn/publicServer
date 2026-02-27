package com.slg.log.auth.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.mysql.entity.BaseMysqlEntity;
import com.slg.log.auth.service.AuthService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 日志系统用户实体
 * 以 username 作为主键，通过 EntityCache 进行缓存管理
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "log_user")
@CacheConfig(maxSize = -1, expireMinutes = -1)
public class LogUserEntity extends BaseMysqlEntity<String> {

    @Column(nullable = false)
    private String password;

    /** 角色：ADMIN / OPERATOR */
    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Override
    public void save() {
        AuthService.getInstance().save(this);
    }

    @Override
    public void saveField(String fieldName) {
        AuthService.getInstance().saveField(this, fieldName);
    }
}
