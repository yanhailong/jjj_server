package com.jjg.game.activity.grandroulette.data;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2026/3/12 17:39
 */
public class GrandRouletteConditionConfig {

    /**
     * 需要达到最低人数
     */
    private int needNum;

    /**
     * 需要最低金币数
     */
    private long needGoldNum;

    /**
     * 需要重置金额
     */
    private BigDecimal needRechargeNum;

    /**
     * 全部条件达成人数
     */
    private int needConcludeNum;

    /**
     * 游戏大类_有效的游戏ID范围|……
     * 1_-1|2_201000_200100|3_300400
     */
    private String gameTypeLimit;

    public GrandRouletteConditionConfig() {
    }

    public GrandRouletteConditionConfig(int needNum, long needGoldNum, BigDecimal needRechargeNum,
                                        int needConcludeNum, String gameTypeLimit) {
        this.needNum = needNum;
        this.needGoldNum = needGoldNum;
        this.needRechargeNum = needRechargeNum;
        this.needConcludeNum = needConcludeNum;
        this.gameTypeLimit = gameTypeLimit;
    }

    public int getNeedNum() {
        return needNum;
    }

    public long getNeedGoldNum() {
        return needGoldNum;
    }

    public BigDecimal getNeedRechargeNum() {
        return needRechargeNum;
    }

    public int getNeedConcludeNum() {
        return needConcludeNum;
    }

    public String getGameTypeLimit() {
        return gameTypeLimit;
    }
}