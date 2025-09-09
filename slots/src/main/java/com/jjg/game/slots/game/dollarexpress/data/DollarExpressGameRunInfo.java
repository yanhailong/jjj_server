package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.dollarexpress.pb.DollarsInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResultLineInfo;
import com.jjg.game.slots.game.dollarexpress.pb.TrainInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/6/12 17:21
 */
public class DollarExpressGameRunInfo extends GameRunInfo<DollarExpressPlayerGameData> {
    //中奖线信息
    private List<ResultLineInfo> awardLineInfos;
    //火车信息
    private List<TrainInfo> trainList;
    //美元信息
    private DollarsInfo dollarsInfo;
    //累计的美钞数量
    private int totalDollars;
    //盘面上所有美金的倍数
    private int dollarsGoldTimes;
    //投资游戏，3次奖励的金币
    private List<Long> investRewardGoldList;
    //投资游戏金火车数
    private int investRewardGoldTrainCount;
    //投资游戏金火车，每节车厢的金币
    private long investRewardGold;
    //投资可选区域
    private List<Integer> choosableAreas;
    //地图
    private boolean allAreaUnLock;

    //奖池金额
    private long mini;
    private long minor;
    private long major;
    private long grand;

    public DollarExpressGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<ResultLineInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<ResultLineInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public List<TrainInfo> getTrainList() {
        return trainList;
    }

    public void setTrainList(List<TrainInfo> trainList) {
        this.trainList = trainList;
    }

    public DollarsInfo getDollarsInfo() {
        return dollarsInfo;
    }

    public void setDollarsInfo(DollarsInfo dollarsInfo) {
        this.dollarsInfo = dollarsInfo;
    }

    public int getTotalDollars() {
        return totalDollars;
    }

    public void setTotalDollars(int totalDollars) {
        this.totalDollars = totalDollars;
    }

    public void addTotalDollars(int totalDollars) {
        this.totalDollars += totalDollars;
    }

    public int getDollarsGoldTimes() {
        return dollarsGoldTimes;
    }

    public void setDollarsGoldTimes(int dollarsGoldTimes) {
        this.dollarsGoldTimes = dollarsGoldTimes;
    }

    public void addDollarsGoldTimes(int times) {
        this.dollarsGoldTimes += times;
    }

    public List<Long> getInvestRewardGoldList() {
        return investRewardGoldList;
    }

    public void setInvestRewardGoldList(List<Long> investRewardGoldList) {
        this.investRewardGoldList = investRewardGoldList;
    }

    public int getInvestRewardGoldTrainCount() {
        return investRewardGoldTrainCount;
    }

    public void setInvestRewardGoldTrainCount(int investRewardGoldTrainCount) {
        this.investRewardGoldTrainCount = investRewardGoldTrainCount;
    }

    public long getInvestRewardGold() {
        return investRewardGold;
    }

    public void setInvestRewardGold(long investRewardGold) {
        this.investRewardGold = investRewardGold;
    }

    public void addTrainInfo(TrainInfo trainInfo) {
        if(this.trainList == null) {
            this.trainList = new ArrayList<>();
        }
        this.trainList.add(trainInfo);
    }
    public void addTrainInfo(List<TrainInfo> list) {
        if(list == null || list.isEmpty()) {
            return;
        }
        if(this.trainList == null) {
            this.trainList = new ArrayList<>();
        }
        this.trainList.addAll(list);
    }

    public List<Integer> getChoosableAreas() {
        return choosableAreas;
    }

    public void setChoosableAreas(List<Integer> choosableAreas) {
        this.choosableAreas = choosableAreas;
    }

    public boolean isAllAreaUnLock() {
        return allAreaUnLock;
    }

    public void setAllAreaUnLock(boolean allAreaUnLock) {
        this.allAreaUnLock = allAreaUnLock;
    }

    public long getMini() {
        return mini;
    }

    public void setMini(long mini) {
        this.mini = mini;
    }

    public long getMinor() {
        return minor;
    }

    public void setMinor(long minor) {
        this.minor = minor;
    }

    public long getMajor() {
        return major;
    }

    public void setMajor(long major) {
        this.major = major;
    }

    public long getGrand() {
        return grand;
    }

    public void setGrand(long grand) {
        this.grand = grand;
    }
}
