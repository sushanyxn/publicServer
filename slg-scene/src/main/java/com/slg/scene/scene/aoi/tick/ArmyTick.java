package com.slg.scene.scene.aoi.tick;

import com.slg.common.executor.TaskModule;
import com.slg.common.tick.AbstractTick;
import com.slg.scene.scene.base.manager.SceneManager;
import com.slg.scene.scene.base.model.Scene;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 军队坐标定时更新任务
 * <p>定时更新所有场景中行军单位的 AOI 视野</p>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Component
public class ArmyTick extends AbstractTick {

    @Autowired
    private SceneManager sceneManager;

    @Override
    public void tick() {
        for (Scene scene : sceneManager.getScenes().values()) {
            scene.getAoiController().armyTick();
        }
    }

    @Override
    public TaskModule getTaskModule() {
        return TaskModule.SCENE;
    }

    @Override
    public long getInitDelayTime() {
        return 305;
    }

    @Override
    public long getTickTime() {
        return 300;
    }
}
