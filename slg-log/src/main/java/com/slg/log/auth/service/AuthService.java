package com.slg.log.auth.service;

import com.slg.entity.cache.anno.EntityCacheInject;
import com.slg.entity.cache.model.EntityCache;
import com.slg.log.auth.entity.LogUserEntity;
import com.slg.log.auth.security.JwtTokenProvider;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证与用户管理服务
 * 通过 EntityCache 提供用户的查询、创建、修改等功能
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Getter
    private static AuthService instance;

    @EntityCacheInject
    private EntityCache<LogUserEntity> userCache;

    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    private void init() {
        instance = this;
    }

    /**
     * 全量加载用户数据到缓存
     */
    public void loadAll() {
        userCache.loadAll();
    }

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名（即主键）
     * @return 用户实体，不存在返回 null
     */
    public LogUserEntity findByUsername(String username) {
        return userCache.findById(username);
    }

    /**
     * 创建用户（插入缓存和数据库）
     *
     * @param user 用户实体
     * @return 插入后的实体
     */
    public LogUserEntity create(LogUserEntity user) {
        return userCache.insert(user);
    }

    /**
     * 用户登录，返回 JWT Token
     *
     * @param username 用户名
     * @param password 密码
     * @return token 字符串，登录失败返回 null
     */
    public String login(String username, String password) {
        LogUserEntity user = userCache.findById(username);
        if (user == null || !user.getEnabled()) {
            return null;
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }
        user.setLastLoginTime(LocalDateTime.now());
        userCache.save(user);
        return tokenProvider.generateToken(user.getId(), user.getRole());
    }

    /**
     * 创建新用户
     *
     * @param username 用户名（即主键）
     * @param password 密码
     * @param role     角色
     * @return 创建后的用户实体
     */
    public LogUserEntity createUser(String username, String password, String role) {
        if (userCache.findById(username) != null) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        LogUserEntity user = new LogUserEntity();
        user.setId(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        return userCache.insert(user);
    }

    /**
     * 获取所有用户列表
     */
    public List<LogUserEntity> listUsers() {
        return new ArrayList<>(userCache.getAllCache());
    }

    /**
     * 修改用户密码
     *
     * @param username    用户名
     * @param newPassword 新密码
     */
    public void changePassword(String username, String newPassword) {
        LogUserEntity user = userCache.findById(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + username);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userCache.save(user);
    }

    /**
     * 启用/禁用用户
     *
     * @param username 用户名
     * @param enabled  是否启用
     */
    public void setUserEnabled(String username, boolean enabled) {
        LogUserEntity user = userCache.findById(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + username);
        }
        user.setEnabled(enabled);
        userCache.save(user);
    }

    /**
     * 删除用户
     *
     * @param username 用户名
     */
    public void deleteUser(String username) {
        userCache.deleteById(username);
    }

    /**
     * 保存用户实体
     */
    public LogUserEntity save(LogUserEntity user) {
        return userCache.save(user);
    }

    /**
     * 保存用户单个字段
     */
    public void saveField(LogUserEntity user, String fieldName) {
        userCache.saveField(user, fieldName);
    }
}
