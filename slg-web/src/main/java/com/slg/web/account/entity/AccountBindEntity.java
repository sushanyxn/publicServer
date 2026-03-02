package com.slg.web.account.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.mysql.entity.BaseMysqlEntity;
import com.slg.web.account.service.AccountBindService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 账号绑定实体
 * 记录平台唯一 ID 与 Account 的绑定关系
 * 主键格式：{platform}_{platformId}，platformId 中不可含有下划线 _
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
public class AccountBindEntity extends BaseMysqlEntity<String> {

    /**
     * 主键：{platform}_{platformId}
     * 由 {@link #buildKey(int, String)} 生成，不使用自增
     */
    @Override
    @Column(name = "id", length = 320)
    public String getId() {
        return id;
    }

    /** 平台类型 {@link com.slg.common.constant.PlatformType} */
    @Column(nullable = false)
    private int platform;

    /** 平台唯一 ID（如设备 ID、Facebook UID、Apple ID 等，不可含 _） */
    @Column(name = "platform_id", length = 256, nullable = false)
    private String platformId;

    /** 绑定时间 */
    @Column(name = "bind_time")
    private LocalDateTime bindTime;

    /** 关联的 Account ID */
    @Column(name = "acc_id", nullable = false)
    private long accId;

    /**
     * 构建绑定主键
     *
     * @param platform   平台类型
     * @param platformId 平台用户 ID（不可含 _）
     * @return 主键字符串
     * @throws IllegalArgumentException 若 platformId 含有 _
     */
    public static String buildKey(int platform, String platformId) {
        if (platformId != null && platformId.contains("_")) {
            throw new IllegalArgumentException("platformId 中不可含有下划线 _，实际值：" + platformId);
        }
        return platform + "_" + platformId;
    }

    @Override
    public void save() {
        AccountBindService.getInstance().save(this);
    }

    @Override
    public void saveField(String fieldName) {
        AccountBindService.getInstance().saveField(this, fieldName);
    }
}
