package com.jjg.game.poker.game.common.config;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.exception.GameSampleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Repository;

/**
 * 牌桌类配置表管理
 *
 * @author 2CL
 */
@Repository
@Order(1)
public class BasePokerSampleLoader implements SmartLifecycle, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(BasePokerSampleLoader.class);
    @Autowired
    private BasePokerSampleManager basePokerSampleManager;
    private boolean isRunning;
    private ApplicationContext applicationContext;

    @Override
    public void start() {
        if (isRunning) {
            return;
        }
        try {
            CommonUtil.setContext(applicationContext);
            basePokerSampleManager.init();
        } catch (Exception exception) {
            log.error("扑克类的公共配置表加载异常 {}", exception.getMessage(), exception);
            throw new GameSampleException(exception);
        }
        isRunning = true;
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public int getPhase() {
        return 1;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
