package com.jjg.game.core.recharge.event;

import com.jjg.game.core.base.condition.handler.TodayDepositCondition;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.dao.PlayerRechargeFlowDao;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.task.manager.TaskManager;
import com.jjg.game.core.task.param.DefaultTaskConditionParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author lm
 * @date 2026/3/25 09:54
 */
@Component
public class BaseRechargeEvent implements GameEventListener {
    private static final Logger log = LoggerFactory.getLogger(BaseRechargeEvent.class);
    private final PlayerRechargeFlowDao playerRechargeFlowDao;
    private final TaskManager taskManager;
    private final TodayDepositCondition todayDepositCondition;
    private final CountDao countDao;

    public BaseRechargeEvent(PlayerRechargeFlowDao playerRechargeFlowDao, TaskManager taskManager,TodayDepositCondition todayDepositCondition,
                             CountDao countDao) {
        this.playerRechargeFlowDao = playerRechargeFlowDao;
        this.taskManager = taskManager;
        this.todayDepositCondition = todayDepositCondition;
        this.countDao = countDao;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (!(gameEvent instanceof PlayerEventCategory.PlayerRechargeEvent event)) {
            return;
        }
        Order newOlder = event.getOrder();
        Player player = event.getPlayer();
        BigDecimal orderPrice = newOlder.getPrice();
        long orderPlayerId = newOlder.getPlayerId();
        try {
            playerRechargeFlowDao.addRechargeFlow(newOlder);
        } catch (Exception e) {
            log.error("记录玩家充值流水失败 playerId = {},orderId = {}", orderPlayerId, newOlder.getId(), e);
        }
        try {
            todayDepositCondition.addBaseProgress(player.getId(), orderPrice);
        } catch (Exception e) {
            log.error("充值增加今日充值进度异常 playerId = {},orderId = {}", orderPlayerId, newOlder.getId(), e);
        }
        try {
            countDao.incrRechargeInfo(player.getId(), String.valueOf(player.getId()), orderPrice);
        } catch (Exception e) {
            log.error("充值累计统计异常 playerId = {},orderId = {}", orderPlayerId, newOlder.getId(), e);
        }
        Supplier<DefaultTaskConditionParam> paramSupplier = () -> {
            DefaultTaskConditionParam param = new DefaultTaskConditionParam();
            param.setAddValue(orderPrice.multiply(BigDecimal.valueOf(100)).longValue());
            return param;
        };
        taskManager.trigger(orderPlayerId, TaskConstant.ConditionType.PLAYER_PAY, paramSupplier, true);
        taskManager.trigger(orderPlayerId, TaskConstant.ConditionType.PLAYER_SUM_PAY, paramSupplier, true);
    }

    @Override
    public Map<EGameEventType, Integer> evetOrder() {
        return Map.of(EGameEventType.RECHARGE, 9998);
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.RECHARGE);
    }
}
