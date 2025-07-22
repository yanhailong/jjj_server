package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.slots.data.DollarInfo;
import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 美元快递结果库
 * @author 11
 * @date 2025/7/7 18:26
 */
@Document
public class DollarExpressResultLib extends SlotsResultLib<DollarExpressAwardLineInfo> {
    //普通火车信息
    private List<Train> trainList;
    //免费游戏信息
    private Map<Integer,DollarExpressFreeGame> freeGameMap;
    //重转信息
    private Map<Integer,DollarExpressAgainGame> againGameMap;
    //黄金列车车厢节数
    private int goldTrainCount;
    //黄金列车触发局中美金的总倍数
    private int goldTrainAllTimes;

    //美元现金奖励
    private DollarInfo dollarInfo;

    public List<Train> getTrainList() {
        return trainList;
    }

    public void setTrainList(List<Train> trainList) {
        this.trainList = trainList;
    }

    public Map<Integer, DollarExpressFreeGame> getFreeGameMap() {
        return freeGameMap;
    }

    public void setFreeGameMap(Map<Integer, DollarExpressFreeGame> freeGameMap) {
        this.freeGameMap = freeGameMap;
    }

    public Map<Integer, DollarExpressAgainGame> getAgainGameMap() {
        return againGameMap;
    }

    public void setAgainGameMap(Map<Integer, DollarExpressAgainGame> againGameMap) {
        this.againGameMap = againGameMap;
    }

    public DollarInfo getDollarInfo() {
        return dollarInfo;
    }

    /**
     * 添加火车信息
     * @param train
     */
    public void addTrain(Train train) {
        if(this.trainList == null) {
            this.trainList = new ArrayList<>();
        }
        this.trainList.add(train);
    }

    public void addFreeGame(int id,int[] iconArr,List<DollarExpressAwardLineInfo> awardLineInfoList) {
        if(this.freeGameMap == null) {
            this.freeGameMap = new HashMap<>();
        }
        DollarExpressFreeGame freeGame = new DollarExpressFreeGame(id);
        freeGame.setIconArr(iconArr);

        if(awardLineInfoList != null && !awardLineInfoList.isEmpty()) {
            for(DollarExpressAwardLineInfo info : awardLineInfoList){
                times += info.getBaseTimes();
            }
            freeGame.setAwardLineInfoList(awardLineInfoList);
        }

        this.freeGameMap.put(id, freeGame);
    }

    public void addFreeGameGoldTrainCount(int id,List<int[]> list){
        if(list == null || list.isEmpty()) {
            return;
        }
        int[] arr = list.get(0);
        DollarExpressFreeGame freeGame = this.freeGameMap.get(id);
        freeGame.setGoldTrainAllTimes(arr[0]);
        freeGame.setGoldTrainCount(arr[1]);
    }

    public void addAgainGameGoldTrainCount(int id,List<int[]> list){
        if(list == null || list.isEmpty()) {
            return;
        }
        int[] arr = list.get(0);
        DollarExpressAgainGame againGame = this.againGameMap.get(id);
        againGame.setGoldTrainAllTimes(arr[0]);
        againGame.setGoldTrainCount(arr[1]);
    }

    public void addFreeGameTrain(int id,Train train) {
        this.freeGameMap.get(id).addTrain(train);
    }

    public void setDollarCashInfo(DollarInfo dollarInfo) {
        this.dollarInfo = dollarInfo;
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

    public void addFreeGameDollarCashInfo(int id, DollarInfo dollarInfo){
        this.freeGameMap.get(id).setDollarCashInfo(dollarInfo);
    }

    public void initAgainGame(int id,int[] iconArr) {
        if(this.againGameMap == null) {
            this.againGameMap = new HashMap<>();
        }

        DollarExpressAgainGame againGame = new DollarExpressAgainGame();
        againGame.setId(id);
        againGame.setIconArr(iconArr);
        this.againGameMap.put(id, againGame);
    }

    public void addAgainGameTrain(int id,Train train) {
        this.againGameMap.get(id).addTrain(train);
    }

    public DollarExpressResultLib copyBaseData(){
        DollarExpressResultLib lib = new DollarExpressResultLib();
        lib.setId(RandomUtils.getUUid());
        lib.setGameType(this.gameType);
        lib.setRollerMode(this.rollerMode);
        return lib;
    }

    public int getGoldTrainAllTimes() {
        return goldTrainAllTimes;
    }

    public void setGoldTrainAllTimes(int goldTrainAllTimes) {
        this.goldTrainAllTimes = goldTrainAllTimes;
    }

    @Override
    public DollarExpressResultLib clone() throws CloneNotSupportedException {
        try {
            DollarExpressResultLib cloned = (DollarExpressResultLib) super.clone();
            if(this.freeGameMap != null && !this.freeGameMap.isEmpty()) {
                cloned.freeGameMap = new HashMap<>(this.freeGameMap);
            }
            if(this.againGameMap != null && !this.againGameMap.isEmpty()) {
                cloned.againGameMap = new HashMap<>(this.againGameMap);
            }

            if(this.dollarInfo != null){
                cloned.dollarInfo = this.dollarInfo.clone();
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
