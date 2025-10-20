package com.jjg.game.slots.manager;

import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.core.base.condition.ConditionType;
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
 * slots类游戏启动总线类
 *
 * @author 11
 * @date 2025/6/10 16:37
 */
@Component
public class SlotsStartManager implements SmartLifecycle, ApplicationContextAware {
    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;
    @Autowired
    private CoreMarqueeManager marqueeManager;
    @Autowired
    private SlotsFactoryManager slotsFactoryManager;
    @Autowired
    private ActivityManager activityManager;
    @Autowired
    private TaskManager taskManager;
    //上下文
    private ApplicationContext context;

    private boolean running = false;

    @Override
    public void start() {
        //启动基础设施
        this.marsCoreStartService.init(this.context, Collections.emptySet());
        //启动core模块
        this.coreStartService.init(this.context);
        //slots 工厂
        this.slotsFactoryManager.init(this.context);
        //跑马灯
        this.marqueeManager.init();
        //加载活动数据
        activityManager.initData();
        //条件枚举
        ConditionType.initData();
        //加载任务管理器
        taskManager.init();
        running = true;
    }

    @Override
    public void stop() {
        //关闭工厂
        this.slotsFactoryManager.shutdown();
        //关闭core模块
        coreStartService.shutdown();
        //关闭基础设施
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
