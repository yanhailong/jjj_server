package com.jjg.game.core.base.condition.data;


public record PlayerBet(
        int gameType,
        long achievedProcess,
        int times,
        int itemId,
        long winCount
) {
}

