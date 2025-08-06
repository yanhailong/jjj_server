package com.jjg.game.slots.game.dollarexpress.data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/7/10 10:27
 */
public class DollarExpressAgainGame {
    private int id;
    //图标集合
    private int[] iconArr;
    //中奖倍率,该重转游戏的总倍率
    private int times;
    //火车信息
    private List<Train> trainList;
    //黄金列车车厢节数
    private int goldTrainCount;
    //黄金列车触发局中美金的总倍数
    private int goldTrainAllTimes;
    //美元倍数
    private List<Integer> dollarTimesList;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int[] getIconArr() {
        return iconArr;
    }

    public void setIconArr(int[] iconArr) {
        this.iconArr = iconArr;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public List<Train> getTrainList() {
        return trainList;
    }

    public void setTrainList(List<Train> trainList) {
        this.trainList = trainList;
    }

    public int getGoldTrainCount() {
        return goldTrainCount;
    }

    public void setGoldTrainCount(int goldTrainCount) {
        this.goldTrainCount = goldTrainCount;
    }

    public List<Integer> getDollarTimesList() {
        return dollarTimesList;
    }

    public void setDollarTimesList(List<Integer> dollarTimesList) {
        this.dollarTimesList = dollarTimesList;
    }

    public void addTrain(Train train) {
        if(this.trainList == null) {
            this.trainList = new ArrayList<>();
        }
        this.trainList.add(train);
    }

    public void addTimes(long times) {
        this.times += times;
    }

    public int getGoldTrainAllTimes() {
        return goldTrainAllTimes;
    }

    public void setGoldTrainAllTimes(int goldTrainAllTimes) {
        this.goldTrainAllTimes = goldTrainAllTimes;
    }
}
