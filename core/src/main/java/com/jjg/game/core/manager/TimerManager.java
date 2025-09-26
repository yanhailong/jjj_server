package com.jjg.game.core.manager;

import com.jjg.game.core.base.gameevent.ClockEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEventManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * cron定时器
 *
 * @author lm
 * @date 2025/9/25 15:20
 */
@Component
public class TimerManager {
    private final GameEventManager gameEventManager;

    public TimerManager(GameEventManager gameEventManager) {
        this.gameEventManager = gameEventManager;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void onZeroClick() {
        gameEventManager.triggerEvent(new ClockEvent(EGameEventType.CLOCK_EVENT, 0));
    }
}
