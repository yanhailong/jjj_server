package com.jjg.game.sim.manager;

import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.core.config.ConfigManager;
import com.jjg.game.core.handler.CoreMessageHandler;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.service.CoreStartService;
import com.jjg.game.core.task.manager.TaskManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * @author 11
 * @date 2026/5/25
 */
@Component
public class SimStartManager implements SmartLifecycle, ApplicationContextAware {
    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;
    @Autowired
    private SimManager simManager;
    @Autowired
    private CoreMarqueeManager marqueeManager;
    @Autowired
    private ConfigManager configManager;
    @Autowired
    private TaskManager taskManager;
    @Autowired
    private CoreMessageHandler coreMessageHandler;

    private ApplicationContext context;

    private boolean running = false;

    @Override
    public void start() {
        marsCoreStartService.init(this.context, Collections.emptySet());
        configManager.loadAll();
        coreStartService.init(this.context);
        marqueeManager.init();
        coreMessageHandler.init();
        simManager.init();

        running = true;
    }

    @Override
    public void stop() {
        simManager.shutdown();
        taskManager.shutdown();
        coreStartService.shutdown();
        marsCoreStartService.shutdown();
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
}
