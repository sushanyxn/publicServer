package com.slg.log.core.lifecycle;

import com.slg.common.constant.LifecyclePhase;
import com.slg.log.alert.service.AlertRecordService;
import com.slg.log.alert.service.AlertRuleService;
import com.slg.log.auth.entity.LogUserEntity;
import com.slg.log.auth.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 日志服业务初始化生命周期
 * 在 DATABASE 加载完成后执行，负责加载业务数据和创建默认账号
 *
 * @author yangxunan
 * @date 2026-02-26
 */
@Slf4j
@Component
public class LogInitLifeCycle implements SmartLifecycle {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    private volatile boolean running = false;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private AlertRecordService alertRecordService;

    @Override
    public void start() {
        log.debug("[LogInit] 日志服业务初始化开始...");

        authService.loadAll();
        initDefaultAdmin();

        alertRuleService.loadAll();
        alertRecordService.loadAll();
        log.debug("[LogInit] 告警规则和记录数据加载完成");

        running = true;
        log.debug("[LogInit] 日志服业务初始化完成，服务就绪");
    }

    /**
     * 初始化默认管理员账号
     * 当用户表为空时自动创建，确保首次部署即可登录
     */
    private void initDefaultAdmin() {
        if (authService.findByUsername(DEFAULT_ADMIN_USERNAME) != null) {
            return;
        }

        LogUserEntity admin = new LogUserEntity();
        admin.setId(DEFAULT_ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        authService.create(admin);
        log.debug("[LogInit] 默认管理员账号已创建: {}", DEFAULT_ADMIN_USERNAME);
    }

    @Override
    public void stop() {
        log.debug("[LogInit] 日志服正在关闭...");
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.LOG_INIT;
    }
}
