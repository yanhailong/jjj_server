package com.jjg.game.room;

import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.base.condition.ConditionType;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.service.CoreStartService;
import com.jjg.game.room.config.ExcludeServiceFilter;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.room.listener.RoomEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/17 13:25
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@EnableScheduling
@ComponentScan(
        basePackages = "com.jjg.game",
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.CUSTOM,
                        classes = ExcludeServiceFilter.class)
        })
@Order(2)
public class RoomApp implements SmartLifecycle, ApplicationContextAware {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;
    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private RoomEventListener roomEventListener;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private ActivityManager activityManager;
    @Autowired
    private CoreMarqueeManager coreMarqueeManager;
    private ApplicationContext context;

    private boolean running = false;

    public static void main(String[] args) {
        SpringApplication.run(RoomApp.class, args);
    }

    @Override
    public void start() {
        //将代码中支持的游戏类型，和配置中的游戏类型对比，能检查配置是否错误
        if (this.nodeConfig.getGameMajorTypes() == null || this.nodeConfig.getGameMajorTypes().length < 1) {
            log.warn("在 nodeconfig.json的 gameMajorTypes 中没有配置开启哪些游戏");
            return;
        }

        //每个游戏都会实现 IRoomStartListener 这个接口
        Map<String, IRoomStartListener> startListenerMap = this.context.getBeansOfType(IRoomStartListener.class);
        if (startListenerMap.isEmpty()) {
            log.warn("没有找到 IRoomStartListener 的实现类，启动失败....");
            return;
        }

        marsCoreStartService.init(this.context, Collections.emptySet());
        coreStartService.init(this.context);
        roomEventListener.init();
        activityManager.initData();
        coreMarqueeManager.init();
        ConditionType.initData();
        //调用启动方法
        for (Map.Entry<String, IRoomStartListener> en : startListenerMap.entrySet()) {
            en.getValue().start();
        }
        running = true;
    }

    @Override
    public void stop() {
        Map<String, IRoomStartListener> startListenerMap = this.context.getBeansOfType(IRoomStartListener.class);
        for (Map.Entry<String, IRoomStartListener> en : startListenerMap.entrySet()) {
            en.getValue().shutdown();
        }
        coreStartService.shutdown();
        marsCoreStartService.shutdown();
        //定时器停止
        WheelTimerUtil.stop();
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
