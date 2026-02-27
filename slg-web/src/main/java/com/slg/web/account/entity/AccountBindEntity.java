package com.slg.web.account.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.mysql.entity.BaseMysqlEntity;
import com.slg.web.account.service.AccountBindService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 账号绑定实体
 * 记录平台唯一 ID 与 Account 的绑定关系
 * 关系：AccountBind(N) → Account(1)
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "account_bind")
@CacheConfig(maxSize = -1, expireMinutes = -1)
public class AccountBindEntity extends BaseMysqlEntity<Long> {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Override
    public Long getId() {
        return id;
    }

    /** 平台唯一 ID（如设备 ID、Facebook UID、Apple ID 等） */
    @Column(name = "platform_id", length = 256, nullable = false)
    private String platformId;

    /** 平台类型 {@link com.slg.common.constant.PlatformType} */
    @Column(nullable = false)
    private int platform;

    /** 绑定时间 */
    @Column(name = "bind_time")
    private LocalDateTime bindTime;

    /** 关联的 Account ID */
    @Column(name = "acc_id", nullable = false)
    private long accId;

    @Override
    public void save() {
        AccountBindService.getInstance().save(this);
    }

    @Override
    public void saveField(String fieldName) {
        AccountBindService.getInstance().saveField(this, fieldName);
    }
}
