package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.core.data.AbstractGameRunInfo;
import com.jjg.game.slots.data.DollarInfo;
import com.jjg.game.slots.game.dollarexpress.pb.DollarsInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/12 17:21
 */
public class DollarExpressGameRunInfo extends AbstractGameRunInfo {
    private long playerId;
    //中奖倍数
    private int allTimes;
    //总计获得的金币
    private long allWinGold;
    //玩家押注，已经除了100
    private long bet;
    //20个图标
    private int[] iconArr;
    //中奖线信息
    private List<DollarExpressAwardLineInfo> awardLineInfos;
    //火车信息
    private List<Train> trainList;
    //状态
    private int status;
    //美元信息
    private DollarsInfo dollarsInfo;
    //黄金列车车厢数量
    private int goldTrainCount;
    //累计的美钞数量
    private int totalDollars;



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

    public int getAllTimes() {
        return allTimes;
    }

    public void setAllTimes(int allTimes) {
        this.allTimes = allTimes;
    }

    public int[] getIconArr() {
        return iconArr;
    }

    public void setIconArr(int[] iconArr) {
        this.iconArr = iconArr;
    }

    public List<DollarExpressAwardLineInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<DollarExpressAwardLineInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<Train> getTrainList() {
        return trainList;
    }

    public void setTrainList(List<Train> trainList) {
        this.trainList = trainList;
    }

    public DollarsInfo getDollarsInfo() {
        return dollarsInfo;
    }

    public void setDollarsInfo(DollarsInfo dollarsInfo) {
        this.dollarsInfo = dollarsInfo;
    }

    public int getGoldTrainCount() {
        return goldTrainCount;
    }

    public void setGoldTrainCount(int goldTrainCount) {
        this.goldTrainCount = goldTrainCount;
    }

    public int getTotalDollars() {
        return totalDollars;
    }

    public void setTotalDollars(int totalDollars) {
        this.totalDollars = totalDollars;
    }
}
