package com.vegasnight.game.gm.manager;

import com.vegasnight.game.common.service.MarsCoreStartService;
import com.vegasnight.game.core.service.CoreStartService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/29 14:39
 */
@Component
public class GmStartManager implements SmartLifecycle, ApplicationContextAware {

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;

    private ApplicationContext context;

    private boolean running = false;

    @Override
    public void start() {
        marsCoreStartService.init(this.context);
        coreStartService.init(this.context);

        running = true;
    }

    @Override
    public void stop() {
        marsCoreStartService.shutdown();
        coreStartService.shutdown();

        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
