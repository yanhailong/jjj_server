package com.vegasnight.game.account.manager;

import com.vegasnight.game.account.dao.PlayerIdDao;
import com.vegasnight.game.common.curator.MarsCurator;
import com.vegasnight.game.common.service.MarsCoreStartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/26 14:29
 */
@Component
public class AccountStartManager implements SmartLifecycle {
    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private PlayerIdDao playerIdDao;
    @Autowired
    private MarsCurator marsCurator;

    private boolean running;

    @Override
    public void start() {
        this.marsCoreStartService.initTimerCenter();
        this.playerIdDao.init();

        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
