package com.slg.log.auth.repository;

import com.slg.log.auth.entity.LogUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户数据访问层
 *
 * @author yangxunan
 * @date 2026-02-26
 */
public interface LogUserRepository extends JpaRepository<LogUser, Long> {

    Optional<LogUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
