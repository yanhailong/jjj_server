package com.jjg.game.hall.minigame.game.luckytreasure.util;

import com.jjg.game.core.constant.LuckyTreasureConstant;
import com.jjg.game.core.data.LuckyTreasure;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;

import java.util.concurrent.TimeUnit;

/**
 * 夺宝奇兵状态计算工具类
 */
public class LuckyTreasureStatusUtil {

    /**
     * 当前状态 1=可购买,2=等待开奖,3=待领取,4=已领取,5=领奖结束(中奖未领取),6=未中奖
     */
    public static final int STATUS_CAN_BUY = 1;

    /**
     * 等待开奖
     */
    public static final int STATUS_WAIT_DRAW = 2;
    /**
     * 待领取
     */
    public static final int STATUS_WAIT_RECEIVE = 3;
    /**
     * 已领取
     */
    public static final int STATUS_RECEIVED = 4;
    /**
     * 领奖结束
     */
    public static final int STATUS_EXPIRED_WINNER = 5;
    /**
     * 未中奖
     */
    public static final int STATUS_NOT_WINNER = 6;

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
        // 领奖时间
        long rewardTime = 0L;
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(LuckyTreasureConstant.Common.LUCKY_TREASURE_GLOBAL_REWARED_CONFIG_ID);
        if (globalConfigCfg != null && globalConfigCfg.getIntValue() > 1) {
            rewardTime = TimeUnit.SECONDS.toMillis(globalConfigCfg.getIntValue());
        }

        long receiveDeadline = endTime + rewardTime + TimeUnit.MINUTES.toMillis(treasure.getConfig().getCollectTime());

        if (currentTime >= receiveDeadline) {
            return 0;
        }

        return (int) ((receiveDeadline - currentTime) / 1000);
    }

    /**
     * 计算活动实际开奖时间（毫秒）
     */
    public static long calculateRewardTimeMillis(LuckyTreasure luckyTreasure) {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(LuckyTreasureConstant.Common.LUCKY_TREASURE_GLOBAL_REWARED_CONFIG_ID);
        if (globalConfigCfg == null || globalConfigCfg.getIntValue() < 1) {
            return luckyTreasure.getEndTime();
        }

        return luckyTreasure.getEndTime() + TimeUnit.SECONDS.toMillis(globalConfigCfg.getIntValue());
    }

    public static int calculateRewardTimeSecond(LuckyTreasure luckyTreasure) {
        long currentTime = System.currentTimeMillis();
        long time = calculateRewardTimeMillis(luckyTreasure);
        return (int) ((time - currentTime) / 1000);
    }

}
