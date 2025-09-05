package com.jjg.game.activity.privilegecard.data;

import com.jjg.game.activity.common.data.PlayerActivityData;

/**
 * @author lm
 * @date 2025/9/3 17:44
 */
public class PlayerPrivilegeCard extends PlayerActivityData {
    //已经领取金币数
    private long hasClaimNum;
    //上次领取时间
    private long lastClaimTime;
    //购买时间
    private long buyTime;
    //结束时间
    private long endTime;

    public long getHasClaimNum() {
        return hasClaimNum;
    }

    public void setHasClaimNum(long hasClaimNum) {
        this.hasClaimNum = hasClaimNum;
    }

    public long getLastClaimTime() {
        return lastClaimTime;
    }

    public void setLastClaimTime(long lastClaimTime) {
        this.lastClaimTime = lastClaimTime;
    }

    public long getBuyTime() {
        return buyTime;
    }

    public void setBuyTime(long buyTime) {
        this.buyTime = buyTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
