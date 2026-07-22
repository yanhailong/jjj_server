package com.jjg.game.core.base.condition.handler;

import com.jjg.game.core.base.condition.event.BetEvent;
import com.jjg.game.core.base.condition.event.PlayerRechargeEvent;
import com.jjg.game.core.base.condition.numeric.GameConditionEvent;
import com.jjg.game.core.base.condition.numeric.RechargeConditionEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 旧字符串表达式事件到统一 condition 事实事件的适配器。
 * <p>
 * 本类只转换已有业务事实，不解释参数、不判断条件，所有语义都由 core numeric 注册表负责。
 */
final class LegacyConditionEventAdapter {
    private LegacyConditionEventAdapter() {
    }

    static GameConditionEvent game(BetEvent event) {
        return game(event, event.getBetAmount());
    }

    static GameConditionEvent game(BetEvent event, long betAmount) {
        return new GameConditionEvent(
                event.getGameId(), event.getGameType(), event.getRoomType(), event.getItemId(), event.getItemId(),
                betAmount, event.getWinAmount(), 0, betAmount > 0, true,
                0, 0, 0, Set.of(), List.of(), Map.of());
    }

    static RechargeConditionEvent recharge(PlayerRechargeEvent event) {
        return new RechargeConditionEvent(event.getChannelId(), toNonNegativeLong(event.getAmount()));
    }

    /**
     * numeric 进度使用 long；充值流水仍以 BigDecimal 原值持久化，转换值只用于规则过滤和达成判断。
     */
    static long toNonNegativeLong(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return 0;
        }
        if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) >= 0) {
            return Long.MAX_VALUE;
        }
        return value.longValue();
    }
}
