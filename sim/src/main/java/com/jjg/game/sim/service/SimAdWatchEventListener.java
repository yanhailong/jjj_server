package com.jjg.game.sim.service;

import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import org.springframework.stereotype.Component;

import java.util.List;

/** 将活动模块的广告成功事件接入公共广告统计及条件分发。 */
@Component
public class SimAdWatchEventListener implements GameEventListener {
    private final AllianceEventService allianceEventService;

    public SimAdWatchEventListener(AllianceEventService allianceEventService) {
        this.allianceEventService = allianceEventService;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof PlayerEvent event && event.getPlayer() != null) {
            allianceEventService.onAdWatch(event.getPlayer().getId());
        }
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.AD_WATCH);
    }
}
