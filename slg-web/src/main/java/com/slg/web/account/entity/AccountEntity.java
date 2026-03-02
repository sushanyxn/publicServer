package com.slg.web.account.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.mysql.anno.Serialized;
import com.slg.entity.mysql.datatype.DataList;
import com.slg.entity.mysql.entity.BaseMysqlEntity;
import com.slg.web.account.model.RoleBriefInfo;
import com.slg.web.account.service.AccountService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 玩家账号实体
 * 一个 Account 可对应多个 AccountBind（多平台绑定）和多个角色（roleInfoList）
 * roleInfoList 由 game 侧创角回调维护，是登录时获取角色信息的首选来源
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "account")
@CacheConfig(maxSize = -1, expireMinutes = -1)
public class AccountEntity extends BaseMysqlEntity<Long> {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Override
    public Long getId() {
        return id;
    }

    /** 主角色 ID（最近一次登录的角色，0 表示新用户尚未创角） */
    @Column(name = "main_role_id")
    private long mainRoleId;

    /**
     * 账号下所有角色的简要信息列表（JSON 存储）
     * 由 game 侧创角回调写入，登录时直接读取。使用 DataList 避免 Hibernate 将 List 当作集合映射
     */
    @Serialized(elementType = RoleBriefInfo.class)
    @Column(name = "role_info_list", columnDefinition = "json")
    private DataList<RoleBriefInfo> roleInfoList = new DataList<>();

    /** 广告 ID（GAID） */
    @Column(name = "advertising_id", length = 256)
    private String advertisingId;

    /** 上次登录时间 */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    /** 是否需要发送绑定奖励邮件 */
    @Column(name = "need_send_bind_mail")
    private boolean needSendBindMail;

    /** 上次使用的设备 ID */
    @Column(name = "last_device_id", length = 256)
    private String lastDeviceId;

    /** 当前绑定的平台类型 {@link com.slg.common.constant.PlatformType} */
    @Column(name = "curr_platform")
    private int currPlatform;

    /** 包名（bundleId / appKey） */
    @Column(name = "app_key", length = 128)
    private String appKey;

    /** 客户端版本号 */
    @Column(name = "app_version", length = 64)
    private String appVersion;

    /** 国家/地区 */
    @Column(length = 64)
    private String country;

    /** 渠道 */
    @Column(length = 64)
    private String channel;

    /** 上次登录 IP */
    @Column(length = 64)
    private String ip;

    @Override
    public void save() {
        AccountService.getInstance().save(this);
    }

    @Override
    public void saveField(String fieldName) {
        AccountService.getInstance().saveField(this, fieldName);
    }
}
