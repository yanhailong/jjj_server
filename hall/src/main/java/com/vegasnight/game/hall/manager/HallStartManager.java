package com.vegasnight.game.hall.manager;

import com.alibaba.fastjson.JSON;
import com.vegasnight.game.common.service.MarsCoreStartService;
import com.vegasnight.game.core.service.CoreStartService;
import com.vegasnight.game.hall.config.HallConfig;
import com.vegasnight.game.sample.AreaConfigInfo;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/29 14:45
 */
@Component
public class HallStartManager implements SmartLifecycle, ApplicationContextAware {
    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;

    private ApplicationContext context;

    private boolean running = false;

    @Autowired
    private HallConfig hallConfig;

    @Override
    public boolean isAutoStartup() {
        return SmartLifecycle.super.isAutoStartup();
    }

    @Override
    public void start() {
        marsCoreStartService.init(this.context);
        coreStartService.init(this.context);

        System.out.println(JSON.toJSONString(hallConfig,true));
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
}
