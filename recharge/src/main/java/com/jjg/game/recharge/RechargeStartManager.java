package com.jjg.game.recharge;

import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.core.service.CoreStartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * @author 11
 * @date 2025/9/22 19:30
 */
@Component
public class RechargeStartManager implements SmartLifecycle, ApplicationContextAware {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;

    private ApplicationContext context;

    private boolean running = false;

    @Override
    public void start() {
        marsCoreStartService.init(this.context, Collections.emptySet());
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
}
