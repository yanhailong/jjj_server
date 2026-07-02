package com.jjg.game.alliance.data;

/**
 * @author 11
 * @date 2026/7/1
 */
public class AllianceRefreshTaskConfig {
    //用户每日可使用补充联盟任务的道具
    private int itemId;
    //每日可使用的道具次数上限
    private int dailyCountLimit;
    //每次使用所消耗的道具数量
    private int spendCountEach;

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getDailyCountLimit() {
        return dailyCountLimit;
    }

    public void setDailyCountLimit(int dailyCountLimit) {
        this.dailyCountLimit = dailyCountLimit;
    }

    public int getSpendCountEach() {
        return spendCountEach;
    }

    public void setSpendCountEach(int spendCountEach) {
        this.spendCountEach = spendCountEach;
    }
}
