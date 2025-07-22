package com.jjg.game.slots.game.dollarexpress.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 投资游戏中选择小地图奖励信息
 * @author 11
 * @date 2025/7/21 13:48
 */
public class InversGoldTrainRewardData {
    //3个小地图的奖励倍数
    private List<Integer> rewardsTimes;
    //黄金列车的节数
    private int goldTrain;

    public List<Integer> getRewardsTimes() {
        return rewardsTimes;
    }

    public void setRewardsTimes(List<Integer> rewardsTimes) {
        this.rewardsTimes = rewardsTimes;
    }

    public int getGoldTrain() {
        return goldTrain;
    }

    public void setGoldTrain(int goldTrain) {
        this.goldTrain = goldTrain;
    }

    public void addTimes(int times){
        if(this.rewardsTimes == null){
            this.rewardsTimes = new ArrayList<>();
        }
        this.rewardsTimes.add(times);
    }
}
