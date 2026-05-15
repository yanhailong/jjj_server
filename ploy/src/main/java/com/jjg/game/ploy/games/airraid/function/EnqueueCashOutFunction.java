package com.jjg.game.ploy.games.airraid.function;

/**
 * @author 11
 * @date 2026/5/15
 */
@FunctionalInterface
public interface EnqueueCashOutFunction {
    void apply(long playerId, int betIndex, int multiplier, long winAmount);
}
