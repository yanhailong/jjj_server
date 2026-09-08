package com.jjg.game.sim.service;

import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/** 每日签到成功的事实事件，由任务流程维护接取后的进度和完成清理。 */
@Component
public class SimDailySignInEventListener implements GameEventListener {
    private final SimPlayerContextRegistry contextRegistry;
    private final SimTaskService taskService;

    public SimDailySignInEventListener(SimPlayerContextRegistry contextRegistry, SimTaskService taskService) {
        this.contextRegistry = contextRegistry;
        this.taskService = taskService;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (!(gameEvent instanceof PlayerEvent event) || event.getPlayer() == null) {
            return;
        }
        SimPlayerContext ctx = contextRegistry.getContext(event.getPlayer().getId());
        if (ctx != null) {
            taskService.onConditionEvent(ctx, new ActionConditionEvent(
                    ActionConditionEvent.Type.DAILY_SIGN_IN, 0, 0, 0, 1, 0, false));
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.DAILY_SIGN_IN);
    }
}
