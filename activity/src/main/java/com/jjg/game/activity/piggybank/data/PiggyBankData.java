package com.jjg.game.activity.piggybank.data;

import com.jjg.game.activity.common.data.PlayerActivityData;

/**
 * @author lm
 * @date 2025/9/12 17:51
 */
public class PiggyBankData extends PlayerActivityData {
    //购买时间
    private long buyTime;
    //当前进度
    private long progress;
    //填满时间
    private long fullTime;

    public PiggyBankData() {
    }

    public PiggyBankData(long activityId, long round) {
        super(activityId, round);
    }

    public long getBuyTime() {
        return buyTime;
    }

    public void setBuyTime(long buyTime) {
        this.buyTime = buyTime;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public long getFullTime() {
        return fullTime;
    }

    public void setFullTime(long fullTime) {
        this.fullTime = fullTime;
    }
}
