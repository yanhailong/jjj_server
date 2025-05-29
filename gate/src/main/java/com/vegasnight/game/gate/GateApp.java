package com.vegasnight.game.gate;

import com.vegasnight.game.common.redis.RedisConfig;
import com.vegasnight.game.common.service.MarsCoreStartService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 网关服务器启动类
 * @author 11
 * @date 2025/5/23 16:46
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        QuartzAutoConfiguration.class,
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class})
@ComponentScan(basePackages = {"com.vegasnight.game"},excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {RedisConfig.class}))
public class GateApp implements SmartLifecycle, ApplicationContextAware {

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private GateServer gateServer;
    @Autowired
    private GateSessionManager gateSessionManager;

    private boolean running = false;
    private ApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(GateApp.class, args);
    }

    @Override
    public void start() {
        marsCoreStartService.init(this.context,false);
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}

