package com.jjg.game.slots.data;

import com.jjg.game.core.data.AbstractGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResultLineInfo;
import com.jjg.game.slots.game.dollarexpress.pb.SafeBoxInfo;
import com.jjg.game.slots.game.dollarexpress.pb.TrainInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/6/12 17:21
 */
public class GameRunInfo extends AbstractGameRunInfo {
    private long playerId;
    private long allWinGold;
    private List<Integer> intList;
    private List<Long> longList;
    private List<ResultLineInfo> resultLineInfoList;
    private int[] intArray;
    private int specialType;
    private int resultShowId;
    //免费次数
    private int freeCount;
    //火车玩法信息
    private List<TrainInfo> trainInfoList;
    //火车玩法信息
    private TrainInfo trainInfo;
    //免费游戏中是否触发了金火车
    private boolean goldTrainInFree;

    //投资模式中奖金额
    private long investGold1;
    private long investGold2;
    private long investGold3;

    private List<SafeBoxInfo> safeBoxInfoList;


    public GameRunInfo(int code, long playerId) {
        super(code);
        this.playerId = playerId;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public List<Integer> getIntList() {
        return intList;
    }

    public void setIntList(List<Integer> intList) {
        this.intList = intList;
    }

    public List<Long> getLongList() {
        return longList;
    }

    public void setLongList(List<Long> longList) {
        this.longList = longList;
    }

    public List<ResultLineInfo> getResultLineInfoList() {
        return resultLineInfoList;
    }

    public void setResultLineInfoList(List<ResultLineInfo> resultLineInfoList) {
        this.resultLineInfoList = resultLineInfoList;
    }

    public int[] getIntArray() {
        return intArray;
    }

    public void setIntArray(int[] intArray) {
        this.intArray = intArray;
    }

    public int getSpecialType() {
        return specialType;
    }

    public void setSpecialType(int specialType) {
        this.specialType = specialType;
    }

    public int getResultShowId() {
        return resultShowId;
    }

    public void setResultShowId(int resultShowId) {
        this.resultShowId = resultShowId;
    }

    public int getFreeCount() {
        return freeCount;
    }

    public void setFreeCount(int freeCount) {
        this.freeCount = freeCount;
    }

    public List<TrainInfo> getTrainInfoList() {
        return trainInfoList;
    }

    public void setTrainInfoList(List<TrainInfo> trainInfoList) {
        this.trainInfoList = trainInfoList;
    }

    public boolean isGoldTrainInFree() {
        return goldTrainInFree;
    }

    public void setGoldTrainInFree(boolean goldTrainInFree) {
        this.goldTrainInFree = goldTrainInFree;
    }

    public TrainInfo getTrainInfo() {
        return trainInfo;
    }

    public void setTrainInfo(TrainInfo trainInfo) {
        this.trainInfo = trainInfo;
    }

    public long getAllWinGold() {
        return allWinGold;
    }

    public long addAllWinGold(long winGold) {
        this.allWinGold += winGold;
        return this.allWinGold;
    }

    public long getInvestGold1() {
        return investGold1;
    }

    public long getInvestGold2() {
        return investGold2;
    }

    public long getInvestGold3() {
        return investGold3;
    }

    public void setInvestGold(Integer times1, Integer times2, Integer times3,long betValue) {
        if(times1 != null && times1 > 0){
            this.investGold1 = betValue * times1;
        }
        if(times2 != null && times2 > 0){
            this.investGold2 = betValue * times2;
        }
        if(times3 != null && times3 > 0){
            this.investGold3 = betValue * times3;
        }
    }

    public List<SafeBoxInfo> getSafeBoxInfoList() {
        return safeBoxInfoList;
    }

    public void addSafeBoxInfo(SafeBoxInfo safeBoxInfo) {
        if(this.safeBoxInfoList == null){
            this.safeBoxInfoList = new ArrayList<>();
        }
        this.safeBoxInfoList.add(safeBoxInfo);
    }
}
