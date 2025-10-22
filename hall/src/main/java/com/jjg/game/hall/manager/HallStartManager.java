package com.jjg.game.hall.manager;

import com.jjg.game.activity.manager.ActivityManager;
import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.core.base.condition.ConditionType;
import com.jjg.game.core.config.ConfigManager;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.service.CoreStartService;
import com.jjg.game.core.service.LoginConfigService;
import com.jjg.game.hall.casino.manager.CasinoManager;
import com.jjg.game.hall.config.HallConfig;
import com.jjg.game.hall.listener.HallPlayerEventListener;
import com.jjg.game.hall.minigame.MinigameManager;
import com.jjg.game.hall.pointsaward.PointsAwardManager;
import com.jjg.game.hall.service.HallService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Collections;

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
    @Autowired
    private HallService hallService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private CoreMarqueeManager marqueeManager;
    @Autowired
    private CasinoManager casinoManager;
    @Autowired
    private HallPlayerEventListener hallPlayerEventListener;
    @Autowired
    private ActivityManager activityManager;
    @Autowired
    private MinigameManager minigameManager;
    @Autowired
    private ConfigManager configManager;
    @Autowired
    private PointsAwardManager pointsAwardManager;
    @Autowired
    private LoginConfigService loginConfigService;

    private ApplicationContext context;

    private boolean running = false;

    @Override
    public void start() {
        marsCoreStartService.init(this.context, Collections.emptySet());
        coreStartService.init(this.context);
        hallService.init();
        configManager.loadAll();
        hallPlayerEventListener.init();
        marqueeManager.init();
        activityManager.initData();
        ConditionType.initData();
        minigameManager.init();
        pointsAwardManager.init();
        loginConfigService.init();
        running = true;
    }

    @Override
    public void stop() {
        marsCoreStartService.shutdown();
        coreStartService.shutdown();
        casinoManager.shutdown();
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
