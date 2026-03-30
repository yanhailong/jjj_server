package com.jjg.game.slots.game.wealthbank.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.wealthbank.pb.WealthBankDollarsInfo;
import com.jjg.game.slots.game.wealthbank.pb.WealthBankResultLineInfo;
import com.jjg.game.slots.game.wealthbank.pb.WealthBankTrainInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/6/12 17:21
 */
public class WealthBankGameRunInfo extends GameRunInfo<WealthBankPlayerGameData> {
    //中奖线信息
    private List<WealthBankResultLineInfo> awardLineInfos;
    //火车信息
    private List<WealthBankTrainInfo> trainList;
    //美元信息
    private WealthBankDollarsInfo wealthBankDollarsInfo;
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

    public WealthBankGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<WealthBankResultLineInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<WealthBankResultLineInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public List<WealthBankTrainInfo> getTrainList() {
        return trainList;
    }

    public void setTrainList(List<WealthBankTrainInfo> trainList) {
        this.trainList = trainList;
    }

    public WealthBankDollarsInfo getDollarsInfo() {
        return wealthBankDollarsInfo;
    }

    public void setDollarsInfo(WealthBankDollarsInfo wealthBankDollarsInfo) {
        this.wealthBankDollarsInfo = wealthBankDollarsInfo;
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

    public void addTrainInfo(WealthBankTrainInfo wealthBankTrainInfo) {
        if(this.trainList == null) {
            this.trainList = new ArrayList<>();
        }
        this.trainList.add(wealthBankTrainInfo);
    }
    public void addTrainInfo(List<WealthBankTrainInfo> list) {
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
}
