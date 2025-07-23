package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.core.data.AbstractGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.pb.DollarsInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResultLineInfo;
import com.jjg.game.slots.game.dollarexpress.pb.TrainInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/12 17:21
 */
public class DollarExpressGameRunInfo extends AbstractGameRunInfo {
    private long playerId;
    //标准池子中奖倍数
    private int bigPoolTimes;
    //小池子中奖
    private Map<Integer,Long> smallPoolRewardMap;
    //玩家之前的金币
    private long beforeGold;
    //总计获得的金币
    private long allWinGold;
    //玩家当前的金币
    private long afterGold;
    //单线押分
    private long stake;
    //玩家押注，已经除了100
    private long bet;
    //20个图标
    private int[] iconArr;
    //中奖线信息
    private List<ResultLineInfo> awardLineInfos;
    //火车信息
    private List<TrainInfo> trainList;
    //状态
    private int status;
    //美元信息
    private DollarsInfo dollarsInfo;
    //累计的美钞数量
    private int totalDollars;
    //剩余免费次数
    private int remainFreeCount;
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


    public DollarExpressGameRunInfo(int code, long playerId) {
        super(code);
        this.playerId = playerId;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public long getAllWinGold() {
        return allWinGold;
    }

    public void setAllWinGold(long allWinGold) {
        this.allWinGold = allWinGold;
    }

    public long getBet() {
        return bet;
    }

    public void setBet(long bet) {
        this.bet = bet;
    }

    public int getBigPoolTimes() {
        return bigPoolTimes;
    }

    public void setBigPoolTimes(int bigPoolTimes) {
        this.bigPoolTimes = bigPoolTimes;
    }

    public void addBigPoolTimes(int allTimes) {
        this.bigPoolTimes += allTimes;
    }

    public int[] getIconArr() {
        return iconArr;
    }

    public void setIconArr(int[] iconArr) {
        this.iconArr = iconArr;
    }

    public List<ResultLineInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<ResultLineInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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

    public int getRemainFreeCount() {
        return remainFreeCount;
    }

    public void setRemainFreeCount(int remainFreeCount) {
        this.remainFreeCount = remainFreeCount;
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

    public long getStake() {
        return stake;
    }

    public void setStake(long stake) {
        this.stake = stake;
    }

    public Map<Integer, Long> getSmallPoolRewardMap() {
        return smallPoolRewardMap;
    }

    public void setSmallPoolRewardMap(Map<Integer, Long> smallPoolRewardMap) {
        this.smallPoolRewardMap = smallPoolRewardMap;
    }

    public List<Long> getInvestRewardGoldList() {
        return investRewardGoldList;
    }

    public void setInvestRewardGoldList(List<Long> investRewardGoldList) {
        this.investRewardGoldList = investRewardGoldList;
    }

    public void addAllWinGold(long winGold) {
        this.allWinGold += winGold;
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

    public void addSmallPoolReward(int poolId,long value) {
        if(this.smallPoolRewardMap == null) {
            this.smallPoolRewardMap = new HashMap<>();
        }
        this.smallPoolRewardMap.put(poolId,value);
    }

    public long getBeforeGold() {
        return beforeGold;
    }

    public void setBeforeGold(long beforeGold) {
        this.beforeGold = beforeGold;
    }

    public long getAfterGold() {
        return afterGold;
    }

    public void setAfterGold(long afterGold) {
        this.afterGold = afterGold;
    }

    public void addTrainInfo(TrainInfo trainInfo) {
        if(this.trainList == null) {
            this.trainList = new ArrayList<>();
        }
        this.trainList.add(trainInfo);
    }

    public long getGoldByPoolId(int poolId) {
        if(this.smallPoolRewardMap == null) {
            return 0;
        }
        return this.smallPoolRewardMap.get(poolId);
    }

    public List<Integer> getChoosableAreas() {
        return choosableAreas;
    }

    public void setChoosableAreas(List<Integer> choosableAreas) {
        this.choosableAreas = choosableAreas;
    }
}
