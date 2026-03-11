package com.slg.scene.core.lifecycle;

import com.slg.common.constant.LifecyclePhase;
import com.slg.common.executor.Executor;
import com.slg.common.executor.core.GlobalScheduler;
import com.slg.common.executor.TaskModule;
import com.slg.common.log.LoggerUtil;
import com.slg.common.util.RandomUtil;
import com.slg.net.zookeeper.model.ServerType;
import com.slg.net.zookeeper.service.ZookeeperShareService;
import com.slg.scene.SpringContext;
import com.slg.scene.base.model.ScenePlayer;
import com.slg.scene.core.config.SceneServerConfiguration;
import com.slg.scene.net.manager.InnerSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 场景服务器初始化生命周期
 * 负责初始化场景相关业务
 *
 * @author yangxunan
 * @date 2026/02/02
 */
@Component
public class SceneInitLifeCycle implements SmartLifecycle {

    private volatile boolean running = false;

    @Autowired
    private InnerSessionManager innerSessionManager;
    @Autowired
    private ZookeeperShareService shareService;
    @Autowired
    private SceneServerConfiguration sceneServerConfiguration;

    @Override
    public void start() {
        LoggerUtil.debug("场景服务器业务初始化中...");

        // TODO: 在此处添加场景业务初始化逻辑

        // zk 实例
        shareService.createInstance(sceneServerConfiguration.getServerId(), ServerType.SCENE);

        running = true;
        LoggerUtil.debug("场景服务器业务初始化完成");
    }

    @Override
    public void stop() {
        LoggerUtil.debug("场景服务器业务停止中...");

        // 设置关闭标志，防止断线触发宽限期清理逻辑
        innerSessionManager.shutdown();

        // 业务线程池由 KeyedVirtualExecutor/GlobalScheduler 的 @PreDestroy 统一管理



        // zk 实例
        shareService.destroyInstance(sceneServerConfiguration.getServerId(), ServerType.SCENE);

        running = false;
        LoggerUtil.debug("场景服务器业务已停止");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.SCENE_INIT;
    }
}
