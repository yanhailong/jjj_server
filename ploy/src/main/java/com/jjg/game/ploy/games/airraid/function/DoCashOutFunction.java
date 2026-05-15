package com.jjg.game.ploy.games.airraid.function;

import com.jjg.game.ploy.games.airraid.pb.ResAirRaidCashOut;

/**
 * @author 11
 * @date 2026/5/15
 */
@FunctionalInterface
public interface DoCashOutFunction {
    ResAirRaidCashOut doCashOut(long playerId, int betIndex, int multiplier, long now, boolean auto);
}
