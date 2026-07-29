package com.jjg.game.sim.service;

import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 监听角色系统玩家等级变化，触发 Guide.xlsx 中 Condition=4 的新手引导。
 * PathName 不匹配时由 SimGuideService 记录为待触发，进入对应场景后再激活。
 */
@Component
public class SimGuidePlayerLevelEventListener implements GameEventListener {

    private final SimPlayerContextRegistry contextRegistry;
    private final SimGuideService guideService;

    public SimGuidePlayerLevelEventListener(SimPlayerContextRegistry contextRegistry,
                                            SimGuideService guideService) {
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
        guideService.triggerPlayerLevelReached(ctx, event.getPlayer().getLevel(), true);
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.PLAYER_LEVEL);
    }
}
