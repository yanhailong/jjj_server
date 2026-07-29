package com.jjg.game.sim.service;

import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 复用核心功能开放条件所监听的玩家事件，触发 Guide.xlsx 中 Condition=7 的引导。
 */
@Component
public class SimGuideFunctionUnlockEventListener implements GameEventListener {

    private final GameFunctionService gameFunctionService;
    private final SimPlayerContextRegistry contextRegistry;
    private final SimGuideService guideService;

    public SimGuideFunctionUnlockEventListener(GameFunctionService gameFunctionService,
                                               SimPlayerContextRegistry contextRegistry,
                                               SimGuideService guideService) {
        this.gameFunctionService = gameFunctionService;
        this.contextRegistry = contextRegistry;
        this.guideService = guideService;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (!(gameEvent instanceof PlayerEvent event) || event.getPlayer() == null) {
            return;
        }
        SimPlayerContext ctx = contextRegistry.getContext(event.getPlayer().getId());
        if (ctx == null) {
            return;
        }
        guideService.triggerFunctionsUnlocked(ctx,
                gameFunctionService.getOpenedFuncIdList(event.getPlayer()), true);
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return gameFunctionService.needMonitorEvents();
    }
}
