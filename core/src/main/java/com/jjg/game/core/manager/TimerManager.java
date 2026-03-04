package com.jjg.game.core.manager;

import com.jjg.game.core.base.gameevent.ClockEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * cron定时器
 *
 * @author lm
 * @date 2025/9/25 15:20
 */
@Component
public class TimerManager implements GmListener {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final GameEventManager gameEventManager;

    public TimerManager(GameEventManager gameEventManager) {
        this.gameEventManager = gameEventManager;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void onZeroClick() {
        gameEventManager.triggerEvent(new ClockEvent(EGameEventType.CLOCK_EVENT, 0));
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        String gmOrder = gmOrders[0];
        if ("onZeroClick".equals(gmOrder)) {
            log.info("onZeroClick trigger");
            gameEventManager.triggerEvent(new ClockEvent(EGameEventType.CLOCK_EVENT, 0));
            return new CommonResult<>(Code.SUCCESS);
        }
        if ("onZeroClickAt".equalsIgnoreCase(gmOrder)) {
            if (gmOrders.length < 2) {
                return new CommonResult<>(Code.PARAM_ERROR);
            }
            try {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                LocalDate businessDate = LocalDate.parse(gmOrders[1], dateTimeFormatter);
                log.info("onZeroClickAt trigger, businessDate={}", businessDate);
                gameEventManager.triggerEvent(new ClockEvent(EGameEventType.CLOCK_EVENT, 0, businessDate));
                return new CommonResult<>(Code.SUCCESS);
            } catch (Exception e) {
                log.warn("onZeroClickAt 参数错误, date={}", gmOrders[1], e);
                return new CommonResult<>(Code.PARAM_ERROR);
            }
        }
        if ("halfDay".equals(gmOrder)) {
            log.info("halfDay trigger");
            gameEventManager.triggerEvent(new ClockEvent(EGameEventType.CLOCK_EVENT, 12));
            return new CommonResult<>(Code.SUCCESS);
        }
        return null;
    }
}
