package com.slg.web.core.lifecycle;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.log.LoggerUtil;
import com.slg.net.zookeeper.model.ZKConfig;
import com.slg.web.account.service.AccountBindService;
import com.slg.web.account.service.AccountService;
import com.slg.web.account.service.UserService;
import com.slg.web.gm.entity.AdminEntity;
import com.slg.web.gm.service.AdminService;
import com.slg.web.gm.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Web 服业务初始化生命周期
 * 在 DATABASE/REDIS/ZOOKEEPER 加载完成后执行，确认基础设施就绪
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Component
public class WebInitLifeCycle implements SmartLifecycle {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    private volatile boolean running = false;

    @Autowired
    private ZKConfig zkConfig;

    @Autowired
    private AdminService adminService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountBindService accountBindService;

    @Autowired
    private UserService userService;

    @Override
    public void start() {
        LoggerUtil.debug("[WebInit] Web 服业务初始化开始...");

        adminService.loadAll();
        initDefaultAdmin();

        accountService.loadAll();
        accountBindService.loadAll();
        userService.loadAll();
        LoggerUtil.debug("[WebInit] 业务数据已加载: Account, AccountBind, User");

        int gameServerCount = zkConfig.getAllGameServers().size();
        int sceneServerCount = zkConfig.getAllSceneServers().size();
        LoggerUtil.debug("[WebInit] ZKConfig 已加载: GameServer {} 个, SceneServer {} 个",
                gameServerCount, sceneServerCount);

        running = true;
        LoggerUtil.debug("[WebInit] Web 服业务初始化完成，服务就绪");
    }

    /**
     * 初始化默认管理员账号
     * 当 admin 表为空时自动创建，确保首次部署即可登录
     */
    private void initDefaultAdmin() {
        AdminEntity existing = adminService.findByUserName(DEFAULT_ADMIN_USERNAME);
        if (existing != null) {
            return;
        }

        String salt = ShiroUtils.generateSalt();
        String hashedPassword = ShiroUtils.hashPassword(DEFAULT_ADMIN_PASSWORD, salt);

        AdminEntity admin = new AdminEntity();
        admin.setId(DEFAULT_ADMIN_USERNAME);
        admin.setSalt(salt);
        admin.setPassword(hashedPassword);
        admin.setEnabled(true);
        admin.setRoles(List.of("ADMIN"));

        adminService.create(admin);
        LoggerUtil.debug("[WebInit] 默认管理员账号已创建: {}", DEFAULT_ADMIN_USERNAME);
    }

    @Override
    public void stop() {
        LoggerUtil.debug("[WebInit] Web 服正在关闭...");
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.WEB_INIT;
    }
}
