package com.jjg.game.core.base.condition.numeric;

/** 一次游戏结算产生的实际赢钱事实。 */
public interface GameWinEvent extends ConditionEvent {
    int gameId();

    int winItemId();

    long win();

    default boolean matchesGame(long configuredGameId) {
        return configuredGameId <= 0 || configuredGameId == gameId();
    }
}
