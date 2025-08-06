package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.slots.data.DollarInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 免费游戏信息
 * @author 11
 * @date 2025/7/8 13:53
 */
public class DollarExpressFreeGame {
    private int id;
    //图标集合
    private int[] iconArr;
    //中奖倍率，这次免费游戏的总倍数
    private long times;
    //中奖线信息
    private List<DollarExpressAwardLineInfo> awardLineInfoList;
    //火车信息
    private List<Train> trainList;
    //黄金列车车厢节数
    private int goldTrainCount;
    //黄金列车触发局中美金的总倍数
    private int goldTrainAllTimes;
    //美元现金奖励
    private DollarInfo dollarInfo;

    public DollarExpressFreeGame() {
    }

    public DollarExpressFreeGame(int id) {
        this.id = id;
    }

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

    public long getTimes() {
        return times;
    }

    public void setTimes(long times) {
        this.times = times;
    }

    public List<DollarExpressAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<DollarExpressAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
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

    public void setDollarInfo(DollarInfo dollarInfo) {
        this.dollarInfo = dollarInfo;
    }

    public DollarInfo getDollarInfo() {
        return dollarInfo;
    }

    public void setDollarCashInfo(DollarInfo dollarInfo) {
        this.dollarInfo = dollarInfo;
    }

    public void addTrain(Train train) {
        if(this.trainList == null){
            this.trainList = new ArrayList<>();
        }
        this.trainList.add(train);
    }

    public void addTimes(long times){
        this.times += times;
    }

    public int getGoldTrainAllTimes() {
        return goldTrainAllTimes;
    }

    public void setGoldTrainAllTimes(int goldTrainAllTimes) {
        this.goldTrainAllTimes = goldTrainAllTimes;
    }
}
