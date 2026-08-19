package com.jjg.game.core.base.condition.numeric;

/** 非 slots 游戏向任务系统上报的单次赢钱事件。 */
public record GameWinConditionEvent(int gameId, int winItemId, long win) implements GameWinEvent {
}
