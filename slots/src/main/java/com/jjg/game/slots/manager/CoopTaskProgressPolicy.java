package com.jjg.game.slots.manager;

import com.jjg.game.sim.constant.CoopTaskConst;
import com.jjg.game.sim.data.CoopTaskRule;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.GameRunInfo;

/**
 * 协作任务共享目标进度计算。
 */
final class CoopTaskProgressPolicy {

    private CoopTaskProgressPolicy() {
    }

    static long delta(CoopTaskRule rule, int statusBefore, GameRunInfo<?> spin) {
        if (rule == null || spin == null) {
            return 0;
        }
        return switch (rule.conditionId()) {
            case CoopTaskConst.Condition.SPECIAL_MODE_COUNT -> statusBefore == SlotsConst.Status.NORMAL
                    && spin.getResultLib() != null
                    && spin.getResultLib().getLibTypeSet() != null
                    && spin.getResultLib().getLibTypeSet().contains(rule.modeId()) ? 1 : 0;
            case CoopTaskConst.Condition.TOTAL_WIN -> Math.max(0, spin.getAllWinGold());
            case CoopTaskConst.Condition.BIG_WIN_COUNT -> spin.getAllWinTimes() >= rule.modeId() ? 1 : 0;
            case CoopTaskConst.Condition.TOTAL_BET -> Math.max(0, spin.getStake());
            default -> 0;
        };
    }

    static long addSaturated(long current, long delta) {
        if (delta <= 0) {
            return current;
        }
        return current > Long.MAX_VALUE - delta ? Long.MAX_VALUE : current + delta;
    }
}

