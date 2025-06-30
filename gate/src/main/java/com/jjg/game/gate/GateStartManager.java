package com.jjg.game.gate;

import com.jjg.game.common.service.MarsCoreStartService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * @author 11
 * @date 2025/5/29 14:42
 */
@Component
public class GateStartManager implements SmartLifecycle, ApplicationContextAware {

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private GateServer gateServer;
    @Autowired
    private GateSessionManager gateSessionManager;

    private boolean running = false;
    private ApplicationContext context;

    @Override
    public void start() {
        marsCoreStartService.init(this.context,false, Collections.emptySet());
        gateSessionManager.init();
        gateServer.init();

        running = true;
    }

    @Override
    public void stop() {
        gateSessionManager.shutdown();
        marsCoreStartService.shutdown();

        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
