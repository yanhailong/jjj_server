package com.jjg.game.hall.pointsaward;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.core.base.gameevent.ClockEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.task.service.TaskService;
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
     * 任务服务
     */
    private final TaskService taskService;

    private final ClusterSystem clusterSystem;

    public PointsAwardManager(PointsAwardTurntableService pointsAwardTurntableService,
                              PointsAwardSignInManager pointsAwardSignInManager,
                              ClusterSystem clusterSystem,
                              TaskService taskService) {
        this.pointsAwardTurntableService = pointsAwardTurntableService;
        this.pointsAwardSignInManager = pointsAwardSignInManager;
        this.taskService = taskService;
        this.clusterSystem = clusterSystem;
    }

    public void init() {
        pointsAwardSignInManager.init();
        pointsAwardTurntableService.init();
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
            } else if (hour == 12) {
                //检测玩家任务
                clusterSystem.getAllOnlinePlayerId().forEach(taskService::checkTask);
            }
        }
    }

    /**
     * 需要监听的事件类型, 根据实际需要监听的类型写入，通过配置表配置或者手动配置，需尽量避免写入无关事件类型
     *
     * @return 事件类型列表
     */
    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.CLOCK_EVENT);
    }
}
