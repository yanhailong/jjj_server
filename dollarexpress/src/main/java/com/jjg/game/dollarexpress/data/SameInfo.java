package com.jjg.game.dollarexpress.data;

import java.math.BigDecimal;

/**
 * @author 11
 * @date 2025/6/18 18:29
 */
public class SameInfo {
    private BigDecimal wildAllTimes;
    private int baseIconId;
    private boolean same;

    public BigDecimal getWildAllTimes() {
        return wildAllTimes;
    }

    public void setWildAllTimes(BigDecimal wildAllTimes) {
        this.wildAllTimes = wildAllTimes;
    }

    public int getBaseIconId() {
        return baseIconId;
    }

    public void setBaseIconId(int baseIconId) {
        this.baseIconId = baseIconId;
    }

    public boolean isSame() {
        return same;
    }

    public void setSame(boolean same) {
        this.same = same;
    }

    public void addTimes(BigDecimal times){
        if(times == null){
            return;
        }
        if(this.wildAllTimes == null){
            this.wildAllTimes = times;
        }else {
            this.wildAllTimes.add(times);
        }
    }
}
