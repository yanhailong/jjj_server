package com.jjg.game.slots.data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/7/2 11:46
 */
public class SlotsResultInfo {
    //游戏类型
    private int gameType;
    //滚轴模式
    private int rollerMode;
    //滚轴id
    private int rollerId;
    //图标集合
    private int[] iconArr;
    //总的中奖倍率
    private int times;
    //中奖线信息
    private List<AwardLineInfo> awardLineInfoList;
    //特殊游戏类型 0.免费旋转  1.开宝箱  2.重新旋转
    private int auxiliaryType;
    //奖励A 的id
    private List<Integer> awardAIdList;
    //奖励c信息列表 [0] = 押分类型， [1] = 倍数
    private List<int[]> awardCList;

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getRollerMode() {
        return rollerMode;
    }

    public void setRollerMode(int rollerMode) {
        this.rollerMode = rollerMode;
    }

    public int getRollerId() {
        return rollerId;
    }

    public void setRollerId(int rollerId) {
        this.rollerId = rollerId;
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

    public List<AwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<AwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }

    public int getAuxiliaryType() {
        return auxiliaryType;
    }

    public void setAuxiliaryType(int auxiliaryType) {
        this.auxiliaryType = auxiliaryType;
    }

    public List<Integer> getAwardAIdList() {
        return awardAIdList;
    }

    public void setAwardAIdList(List<Integer> awardAIdList) {
        this.awardAIdList = awardAIdList;
    }

    public List<int[]> getAwardCList() {
        return awardCList;
    }

    public void setAwardCList(List<int[]> awardCList) {
        this.awardCList = awardCList;
    }

    public void addAwardAId(int awardAId) {
        if(this.awardAIdList == null){
            this.awardAIdList = new ArrayList<>();
        }
        this.awardAIdList.add(awardAId);
    }

    public void addAwardC(int[] awardC) {
        if(this.awardCList == null) {
            this.awardCList = new ArrayList<>();
        }
        this.awardCList.add(awardC);
    }

    public void addTimes(int times){
        this.times += times;
    }

    public void addAwardLineInfo(AwardLineInfo awardLineInfo){
        if(this.awardLineInfoList == null){
            this.awardLineInfoList = new ArrayList<>();
        }
        this.awardLineInfoList.add(awardLineInfo);
    }
}
