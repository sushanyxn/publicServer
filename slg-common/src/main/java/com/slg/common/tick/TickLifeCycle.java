package com.slg.common.tick;

import com.slg.common.constant.LifecyclePhase;
import lombok.Getter;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * @author yangxunan
 * @date 2026/2/3
 */
@Getter
@Component
public class TickLifeCycle implements SmartLifecycle {

    private volatile boolean running = false;

    @Override
    public void start(){
        AbstractTick.startAll();
        running = true;
    }

    @Override
    public void stop(){
        running = false;
        AbstractTick.stopAll();
    }

    @Override
    public int getPhase() {
        return LifecyclePhase.TICK_INIT;
    }
}
