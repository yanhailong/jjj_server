package com.jjg.game.sim.manager;

import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.sim.service.SimVisitRankService;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 拜访赛季榜 leader 调度。
 *
 * @author 11
 * @date 2026/6/30
 */
@Component
public class SimVisitManager implements IGameClusterLeaderListener {
    private static final Logger log = LoggerFactory.getLogger(SimVisitManager.class);

    @Autowired
    private SimVisitRankService rankService;
    @Autowired
    private MarsCurator marsCurator;

    private volatile boolean running;
    private volatile Timeout settleTimeout;

    public void init() {
        running = true;
        //Hall 启动顺序中 leader 选举早于业务 manager 初始化，需要主动补接当前 leader 状态。
        if (marsCurator.isMaster()) {
            isLeader();
        }
    }

    public void shutdown() {
        running = false;
        notLeader();
    }

    @Override
    public void isLeader() {
        if (!running || settleTimeout != null) {
            return;
        }
        settleTimeout = WheelTimerUtil.scheduleAtFixedRate(
                this::safeSettle, 60, 3600, TimeUnit.SECONDS);
    }

    @Override
    public void notLeader() {
        if (settleTimeout != null) {
            settleTimeout.cancel();
            settleTimeout = null;
        }
    }

    private void safeSettle() {
        try {
            //每小时尝试结算上个自然月；赛季标记保证幂等，也能补偿月初停服。
            rankService.settlePreviousSeason();
        } catch (Exception e) {
            log.error("拜访人气榜结算检查异常", e);
        }
    }
}
