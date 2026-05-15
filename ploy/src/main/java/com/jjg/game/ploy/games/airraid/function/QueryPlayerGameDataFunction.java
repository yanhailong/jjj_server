package com.jjg.game.ploy.games.airraid.function;

import com.jjg.game.ploy.games.airraid.data.AirRaidPlayerPloyGameData;

/**
 * @author 11
 * @date 2026/5/15
 */
@FunctionalInterface
public interface QueryPlayerGameDataFunction {
    AirRaidPlayerPloyGameData getPlayerGameData(long playerId);
}
