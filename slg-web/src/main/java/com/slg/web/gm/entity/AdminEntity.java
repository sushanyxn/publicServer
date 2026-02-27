package com.slg.web.gm.entity;

import com.slg.entity.cache.anno.CacheConfig;
import com.slg.entity.mysql.anno.Serialized;
import com.slg.entity.mysql.entity.BaseMysqlEntity;
import com.slg.web.gm.service.AdminService;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * GM 管理员实体
 * 以 userName 作为主键，通过 EntityCache 进行缓存管理
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "admin")
@CacheConfig(maxSize = -1, expireMinutes = -1)
public class AdminEntity extends BaseMysqlEntity<String> {

    @Column(length = 128, nullable = false)
    private String password;

    @Column(length = 64)
    private String salt;

    @Column(length = 128)
    private String email;

    /** 角色列表（JSON 序列化存储） */
    @SuppressWarnings("JpaAttributeTypeInspection")
    @Serialized
    @Column(columnDefinition = "json")
    private List<String> roles;

    /** 权限列表（JSON 序列化存储） */
    @SuppressWarnings("JpaAttributeTypeInspection")
    @Serialized
    @Column(columnDefinition = "json")
    private List<String> permissions;

    /** 是否启用 */
    @Column(nullable = false)
    private boolean enabled = true;

    @Override
    public void save() {
        AdminService.getInstance().save(this);
    }

    @Override
    public void saveField(String fieldName) {
        AdminService.getInstance().saveField(this, fieldName);
    }
}
