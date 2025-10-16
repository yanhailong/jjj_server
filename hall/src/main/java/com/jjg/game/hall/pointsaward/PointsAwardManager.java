package com.jjg.game.hall.pointsaward;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.core.base.gameevent.*;
import com.jjg.game.core.task.service.TaskService;
import com.jjg.game.hall.pointsaward.leaderboard.PointsAwardLeaderboardManager;
import com.jjg.game.hall.pointsaward.signin.PointsAwardSignInManager;
import com.jjg.game.hall.pointsaward.turntable.PointsAwardTurntableService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 积分大奖管理器
 */
@Component
public class PointsAwardManager implements GameEventListener {

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

    /**
     * 任务服务
     */
    private final TaskService taskService;
    private final PointsAwardService pointsAwardService;

    private final ClusterSystem clusterSystem;

    public PointsAwardManager(PointsAwardTurntableService pointsAwardTurntableService,
                              PointsAwardSignInManager pointsAwardSignInManager,
                              ClusterSystem clusterSystem,
                              PointsAwardService pointsAwardService,
                              PointsAwardLeaderboardManager pointsAwardLeaderboardManager,
                              TaskService taskService) {
        this.pointsAwardTurntableService = pointsAwardTurntableService;
        this.pointsAwardSignInManager = pointsAwardSignInManager;
        this.taskService = taskService;
        this.pointsAwardService = pointsAwardService;
        this.pointsAwardLeaderboardManager = pointsAwardLeaderboardManager;
        this.clusterSystem = clusterSystem;
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
            } else if (hour == 12) {
                //检测玩家任务
                clusterSystem.getAllOnlinePlayerId().forEach(taskService::checkTask);
            }
            pointsAwardLeaderboardManager.clock(hour);
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
}
