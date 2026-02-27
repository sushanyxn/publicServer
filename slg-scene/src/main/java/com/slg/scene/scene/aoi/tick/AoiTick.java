package com.slg.scene.scene.aoi.tick;

import com.slg.common.executor.TaskModule;
import com.slg.common.tick.AbstractTick;
import com.slg.scene.scene.base.manager.SceneManager;
import com.slg.scene.scene.base.model.Scene;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AOI定时任务
 * <p>定时触发所有场景的视野更新处理</p>
 * 
 * <p><b>执行频率：</b></p>
 * <ul>
 *   <li>每 300ms 执行一次</li>
 *   <li>批量处理所有积累的视野变化请求</li>
 * </ul>
 * 
 * <p><b>设计考虑：</b></p>
 * <ul>
 *   <li>300ms 的间隔平衡了实时性和性能</li>
 *   <li>过短：增加CPU负担，消息发送频繁</li>
 *   <li>过长：视野更新延迟明显，影响体验</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/3
 */
@Component
public class AoiTick extends AbstractTick {

    @Autowired
    private SceneManager sceneManager;

    /**
     * 定时执行所有场景的AOI更新
     * <p>遍历所有场景，调用其 AOI 控制器的 tick 方法</p>
     */
    @Override
    public void tick() {
        for (Scene scene : sceneManager.getScenes().values()) {
            scene.getAoiController().aoiTick();
        }
    }

    /**
     * 获取任务模块
     * <p>使用场景模块执行，保证线程安全</p>
     *
     * @return 场景任务模块
     */
    @Override
    public TaskModule getTaskModule() {
        return TaskModule.SCENE;
    }

    /**
     * 获取 tick 间隔
     * <p>每 300ms 执行一次 AOI 更新</p>
     *
     * @return 间隔时间（毫秒）
     */
    @Override
    public long getTickTime() {
        return 300;
    }
}
