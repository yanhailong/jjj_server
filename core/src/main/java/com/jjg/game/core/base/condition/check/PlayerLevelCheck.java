package com.jjg.game.core.base.condition.check;

import com.jjg.game.core.base.condition.ConditionCheck;
import com.jjg.game.core.data.Player;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author lm
 * @date 2025/10/17 11:12
 */
public class PlayerLevelCheck implements ConditionCheck {
    @Override
    public boolean check(Object paramObject, Object conditionObject) {
        if (paramObject instanceof Player param && conditionObject instanceof Integer condition) {
            return param.getLevel() >= condition;
        }
        return false;
    }

    @Override
    public BigDecimal getProgress(Object param) {
        if (param instanceof Player player) {
            return BigDecimal.valueOf(player.getLevel());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public Integer analysisCondition(List<String> condition) {
        if (condition.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(condition.getFirst());
    }
}
