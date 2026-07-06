package com.jjg.game.slots.game.lianHuanDuoBao.data;

/**
 * 宝箱开启奖励信息（连环夺宝特有）
 *
 * @author lm
 * @date 2026/6/2
 */
public class LianHuanDuoBaoChestReward {
    //该宝箱在盘面上的格子索引
    private int chestIndex;
    //奖金倍数（实际奖金 = 倍数 × 单押注金额）
    private int rewardTimes;
    //本宝箱是否额外掉了龙珠
    private boolean dropDragonBall;

    public int getChestIndex() {
        return chestIndex;
    }

    public void setChestIndex(int chestIndex) {
        this.chestIndex = chestIndex;
    }

    public int getRewardTimes() {
        return rewardTimes;
    }

    public void setRewardTimes(int rewardTimes) {
        this.rewardTimes = rewardTimes;
    }

    public boolean isDropDragonBall() {
        return dropDragonBall;
    }

    public void setDropDragonBall(boolean dropDragonBall) {
        this.dropDragonBall = dropDragonBall;
    }
}
