package com.jjg.game.alliance.service;

import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.data.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充值事件 -> 联盟任务进度 (条件 12307: 接取任务后个人累计充值)。
 * <p>
 * 复用 core 的 {@link GameEventListener} 派发(与 {@code BaseRechargeEvent} 同一入口):
 * 金额与 core 充值条件口径一致取订单价 *100(分), 渠道取 payChannel。派发后交给
 * {@link AllianceEventService} —— 其内部按"玩家无联盟/未接任务"短路, 绝大多数充值零成本跳过。
 * <p>
 * "接取任务后累计"语义由 {@code AllianceTaskService#acceptTask} 接取时清空进度计数天然满足,
 * 无需额外的接取时间戳比对。
 *
 * @author 11
 */
@Component
public class AllianceRechargeEventListener implements GameEventListener {

    @Autowired
    private AllianceEventService allianceEventService;

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (!(gameEvent instanceof PlayerEventCategory.PlayerRechargeEvent event)) {
            return;
        }
        Order order = event.getOrder();
        if (order == null || order.getPrice() == null) {
            return;
        }
        //与 BaseRechargeEvent 口径一致: 金额单位为分 (元 *100), 与 12307 目标值(如 500=5$)对齐
        long amountCents = order.getPrice().multiply(BigDecimal.valueOf(100)).longValue();
        if (amountCents <= 0) {
            return;
        }
        allianceEventService.onRecharge(order.getPlayerId(), order.getPayChannel(), amountCents);
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.RECHARGE);
    }
}
