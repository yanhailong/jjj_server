package com.jjg.game.hall.minigame.game.luckytreasure.util;

import com.jjg.game.hall.minigame.game.luckytreasure.data.LuckyTreasure;

import java.util.concurrent.TimeUnit;

/**
 * 夺宝奇兵状态计算工具类
 */
public class LuckyTreasureStatusUtil {

    /**
     * 当前状态 1=可购买,2=等待开奖,3=待领取,4=已领取,5=领奖结束(中奖未领取),6=未中奖
     */
    public static final int STATUS_CAN_BUY = 1;
    public static final int STATUS_WAIT_DRAW = 2;
    public static final int STATUS_WAIT_RECEIVE = 3;
    public static final int STATUS_RECEIVED = 4;
    public static final int STATUS_EXPIRED_WINNER = 5;
    public static final int STATUS_NOT_WINNER = 6;

    /**
     * 计算夺宝奇兵当前状态
     */
    public static int calculateStatus(LuckyTreasure treasure, long playerId) {
        long currentTime = System.currentTimeMillis();

        // 如果还没到结束时间，可以购买
        if (treasure.getEndTime() > 0 && currentTime < treasure.getEndTime()) {
            return STATUS_CAN_BUY;
        }

        // 如果已售完但还没开奖，等待开奖
        if (treasure.getSoldCount() >= treasure.getConfig().getTotal() && treasure.getAwardPlayerId() == 0) {
            return STATUS_WAIT_DRAW;
        }

        // 如果已开奖
        if (treasure.getAwardPlayerId() > 0) {
            //自己是领奖人
            if (treasure.getAwardPlayerId() == playerId) {
                // 检查是否已领取
                if (treasure.isReceived()) {
                    return STATUS_RECEIVED;
                }
                // 检查是否超过领奖时间
                long receiveDeadline = treasure.getEndTime() + TimeUnit.MINUTES.toMillis(treasure.getConfig().getCollectTime());
                if (currentTime > receiveDeadline) {
                    return STATUS_EXPIRED_WINNER;
                }
                return STATUS_WAIT_RECEIVE;
            } else {
                return STATUS_NOT_WINNER;
            }
        }
        // 默认状态
        return STATUS_NOT_WINNER;
    }


    /**
     * 计算开奖倒计时（秒）
     */
    public static int calculateCountDown(LuckyTreasure treasure) {
        long currentTime = System.currentTimeMillis();
        long endTime = treasure.getEndTime();

        if (currentTime >= endTime) {
            return 0;
        }

        return (int) ((endTime - currentTime) / 1000);
    }

    /**
     * 计算领奖倒计时（秒）
     */
    public static int calculateReceiveCountdown(LuckyTreasure treasure) {
        // 这里可以根据实际业务逻辑计算领奖倒计时
        // 比如开奖后24小时内可以领奖
        long currentTime = System.currentTimeMillis();
        long endTime = treasure.getEndTime();
        long receiveDeadline = endTime + TimeUnit.MINUTES.toMillis(treasure.getConfig().getCollectTime());

        if (currentTime >= receiveDeadline) {
            return 0;
        }

        return (int) ((receiveDeadline - currentTime) / 1000);
    }

}
