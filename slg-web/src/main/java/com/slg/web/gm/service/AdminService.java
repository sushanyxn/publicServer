package com.slg.web.gm.service;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.web.gm.entity.AdminEntity;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GM 管理员服务
 * 通过 EntityCache 提供管理员的查询、创建等功能
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Service
public class AdminService {

    @Getter
    private static AdminService instance;

    @EntityCacheInject
    private EntityCache<AdminEntity> adminEntityCache;

    @PostConstruct
    private void init() {
        instance = this;
    }

    /**
     * 初始化：全量加载管理员数据到缓存
     */
    public void loadAll() {
        adminEntityCache.loadAll();
    }

    /**
     * 根据用户名查找管理员（userName 即主键）
     *
     * @param userName 用户名
     * @return 管理员实体，不存在返回 null
     */
    public AdminEntity findByUserName(String userName) {
        return adminEntityCache.findById(userName);
    }

    /**
     * 获取所有缓存中的管理员
     *
     * @return 管理员列表
     */
    public List<AdminEntity> findAll() {
        Collection<AdminEntity> all = adminEntityCache.getAllCache();
        return new ArrayList<>(all);
    }

    /**
     * 保存管理员（更新）
     *
     * @param admin 管理员实体
     * @return 保存后的实体
     */
    public AdminEntity save(AdminEntity admin) {
        return adminEntityCache.save(admin);
    }

    /**
     * 保存管理员单个字段
     *
     * @param admin 管理员实体
     * @param fieldName 字段名
     */
    public void saveField(AdminEntity admin, String fieldName) {
        adminEntityCache.saveField(admin, fieldName);
    }

    /**
     * 创建管理员
     *
     * @param admin 管理员实体
     * @return 插入后的实体
     */
    public AdminEntity create(AdminEntity admin) {
        return adminEntityCache.insert(admin);
    }
}
