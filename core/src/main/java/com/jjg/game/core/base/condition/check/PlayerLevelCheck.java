package com.jjg.game.core.base.condition.check;

import com.jjg.game.core.base.condition.ConditionCheck;
import com.jjg.game.core.data.Player;

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
    public long getProgress(Object param) {
        if (param instanceof Player player) {
            return player.getLevel();
        }
        return 0;
    }

    @Override
    public Integer analysisCondition(List<Integer> condition) {
        if (condition.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return condition.getFirst();
    }
}
