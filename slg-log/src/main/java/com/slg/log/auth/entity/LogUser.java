package com.slg.log.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 日志系统用户实体
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Entity
@Table(name = "log_user")
@Getter
@Setter
@NoArgsConstructor
public class LogUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    /** 角色：ADMIN / OPERATOR */
    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false)
    private Boolean enabled = true;

    private LocalDateTime createTime;

    private LocalDateTime lastLoginTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
    }
}
