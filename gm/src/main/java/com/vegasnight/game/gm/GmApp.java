package com.vegasnight.game.gm;

import com.vegasnight.game.common.service.MarsCoreStartService;
import com.vegasnight.game.core.service.CoreStartService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author 11
 * @date 2025/5/29 10:02
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@ComponentScan({"com.vegasnight.game"})
public class GmApp implements SmartLifecycle, ApplicationContextAware {
    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;

    private ApplicationContext context;

    private boolean running = false;


    public static void main(String[] args) {
        SpringApplication.run(GmApp.class, args);
    }

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
}
