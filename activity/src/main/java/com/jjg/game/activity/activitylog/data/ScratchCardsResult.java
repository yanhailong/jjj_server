package com.jjg.game.activity.activitylog.data;

import java.util.Map;

/**
 * @author lm
 * @date 2025/9/15 14:13
 */
public class ScratchCardsResult {
    //刮出图标数
    private int iconNum;
    //奖励
    private Map<Integer, Long> rewards;
    //详情id
    private int detailId;

    public ScratchCardsResult() {
    }

    public ScratchCardsResult(int iconNum, Map<Integer, Long> rewards, int detailId) {
        this.iconNum = iconNum;
        this.rewards = rewards;
        this.detailId = detailId;
    }

    public int getIconNum() {
        return iconNum;
    }

    public void setIconNum(int iconNum) {
        this.iconNum = iconNum;
    }

    public Map<Integer, Long> getRewards() {
        return rewards;
    }

    public void setRewards(Map<Integer, Long> rewards) {
        this.rewards = rewards;
    }

    public int getDetailId() {
        return detailId;
    }

    public void setDetailId(int detailId) {
        this.detailId = detailId;
    }
}
