package com.jjg.game.hall.minigame.game.luckytreasure.bean;

/**
 * 夺宝奇兵定时器事件
 */
public record LuckyTreasureTimerEvent(long issueNumber, TimerType timerType) {

    /**
     * 定时器类型
     */
    public enum TimerType {
        /**
         * 活动结束
         */
        ACTIVITY_END
    }
}
