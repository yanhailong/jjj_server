package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * @author lm
 * @date 2026/3/12 15:24
 */
@Document
public class RedeemCodeInfo {
    //礼包id
    @Id
    private long id;
    //奖励数据
    private Map<Integer, Long> rewardsItem;
    //开始时间戳(毫秒)
    private long startTime;
    //结束时间戳(毫秒)
    private long endTime;
    //是否在使用中
    private boolean isUse;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Map<Integer, Long> getRewardsItem() {
        return rewardsItem;
    }

    public void setRewardsItem(Map<Integer, Long> rewardsItem) {
        this.rewardsItem = rewardsItem;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isUse() {
        return isUse;
    }

    public void setUse(boolean use) {
        isUse = use;
    }
}
