package com.jjg.game.hall.pointsaward;

import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.base.gameevent.*;
import com.jjg.game.hall.pointsaward.leaderboard.PointsAwardLeaderboardManager;
import com.jjg.game.hall.pointsaward.signin.PointsAwardSignInManager;
import com.jjg.game.hall.pointsaward.turntable.PointsAwardTurntableService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 积分大奖管理器
 */
@Component
public class PointsAwardManager implements GameEventListener, TimerListener<String> {

    /**
     * 转盘服务
     */
    private final PointsAwardTurntableService pointsAwardTurntableService;

    /**
     * 签到管理器
     */
    private final PointsAwardSignInManager pointsAwardSignInManager;

    /**
     * 排行榜管理器
     */
    private final PointsAwardLeaderboardManager pointsAwardLeaderboardManager;

    private final PointsAwardService pointsAwardService;
    private final TimerCenter timerCenter;

    private TimerEvent<String> delayLoadRankEvent;

    public PointsAwardManager(PointsAwardTurntableService pointsAwardTurntableService,
                              PointsAwardSignInManager pointsAwardSignInManager,
                              PointsAwardService pointsAwardService,
                              PointsAwardLeaderboardManager pointsAwardLeaderboardManager, TimerCenter timerCenter) {
        this.pointsAwardTurntableService = pointsAwardTurntableService;
        this.pointsAwardSignInManager = pointsAwardSignInManager;
        this.pointsAwardService = pointsAwardService;
        this.pointsAwardLeaderboardManager = pointsAwardLeaderboardManager;
        this.timerCenter = timerCenter;
    }

    public void init() {
        pointsAwardService.init();
        pointsAwardSignInManager.init();
        pointsAwardTurntableService.init();
        pointsAwardLeaderboardManager.init();
    }




    /**
     * 处理事件
     *
     * @param gameEvent 事件
     */
    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof ClockEvent clockEvent) {
            int hour = clockEvent.getHour();
            if (hour == 0) {
                pointsAwardSignInManager.daily();
                pointsAwardTurntableService.dailyReset();
                pointsAwardService.daily();
            }
            pointsAwardLeaderboardManager.clock(hour);
            this.delayLoadRankEvent = new TimerEvent<>(this, 20, "delayLoadRank").withTimeUnit(TimeUnit.SECONDS);
            this.timerCenter.add(this.delayLoadRankEvent);
        }
        //玩家充值事件
        else if (gameEvent instanceof PlayerEventCategory.PlayerRechargeEvent rechargeEvent) {
            pointsAwardService.recharge(rechargeEvent.getOrder());
            pointsAwardTurntableService.recharge(rechargeEvent.getOrder());
        }
    }

    /**
     * 需要监听的事件类型, 根据实际需要监听的类型写入，通过配置表配置或者手动配置，需尽量避免写入无关事件类型
     *
     * @return 事件类型列表
     */
    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.CLOCK_EVENT, EGameEventType.RECHARGE);
    }

    @Override
    public void onTimer(TimerEvent<String> e) {
        if(e == this.delayLoadRankEvent){
            pointsAwardLeaderboardManager.cacheRankData();
            this.timerCenter.remove(e);
            this.delayLoadRankEvent = null;
        }
    }
}
