package com.jjg.game.slots.game.wealthbank.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 玩家游戏数据
 * @author 11
 * @date 2025/6/10 18:07
 */
public class WealthBankPlayerGameData extends SlotsPlayerGameData {
    //累计的美钞数量
    private int totalDollars;
    //记录出现可收集美元的局数
    private int addDollarsCount;
    //记录收集美元时的押注总和(用于计算累计的美钞的平均值)
    private long addDollarsTotalStake;
    //是否可以投资
    private AtomicBoolean invers = new AtomicBoolean(false);
    //已经选择的地区
    private Set<Integer> selectedAreaSet;
    //全地图解锁
    private AtomicBoolean allUnLock = new AtomicBoolean(false);
    //免费模式累计奖励
    private long freeModeTotalReward = 0;

    public long getFreeModeTotalReward() {
        return freeModeTotalReward;
    }

    public void setFreeModeTotalReward(long freeModeTotalReward) {
        this.freeModeTotalReward = freeModeTotalReward;
    }

    public void addFreeModeTotalReward(long freeModeTotalReward) {
        this.freeModeTotalReward += freeModeTotalReward;
    }

    public int getTotalDollars() {
        return totalDollars;
    }

    public void setTotalDollars(int totalDollars) {
        this.totalDollars = totalDollars;
    }

    public void addDollasCount(int count){
        this.totalDollars += count;
    }

    public void addDollarsTotalStake(long stake){
        this.addDollarsCount ++;
        this.addDollarsTotalStake += stake;
    }

    public AtomicBoolean getInvers() {
        return invers;
    }

    public void setInvers(AtomicBoolean invers) {
        this.invers = invers;
    }

    public Set<Integer> getSelectedAreaSet() {
        return selectedAreaSet;
    }

    public void setSelectedAreaSet(Set<Integer> selectedAreaSet) {
        this.selectedAreaSet = selectedAreaSet;
    }

    /**
     * 添加已选的地区
     * @param areaId
     */
    public boolean addSelectedArea(int areaId) {
        if(this.selectedAreaSet == null){
            this.selectedAreaSet = new HashSet<>();
        }
        return this.selectedAreaSet.add(areaId);
    }

    public boolean areaSelected(int areaId) {
        if(this.selectedAreaSet == null){
            return false;
        }
        return this.selectedAreaSet.contains(areaId);
    }

    public boolean areaAllUnlock(){
        if(this.selectedAreaSet == null || this.selectedAreaSet.isEmpty()){
            return false;
        }
        return this.selectedAreaSet.size() >= 8;
    }

    public int getAddDollarsCount() {
        return addDollarsCount;
    }

    public void setAddDollarsCount(int addDollarsCount) {
        this.addDollarsCount = addDollarsCount;
    }

    public long getAddDollarsTotalStake() {
        return addDollarsTotalStake;
    }

    public void setAddDollarsTotalStake(long addDollarsTotalStake) {
        this.addDollarsTotalStake = addDollarsTotalStake;
    }

    /**
     * 清除投资小游戏相关
     */
    public void clearInvers(){
        this.addDollarsCount = 0;
        this.addDollarsTotalStake = 0;
    }

    public AtomicBoolean getAllUnLock() {
        return allUnLock;
    }

    public void setAllUnLock(AtomicBoolean allUnLock) {
        this.allUnLock = allUnLock;
    }

    @Override
    public <T extends SlotsPlayerGameDataDTO> T converToDto(Class<T> cla) throws Exception {
        T dto = super.converToDto(cla);
        WealthBankPlayerGameDataDTO dollarDto = (WealthBankPlayerGameDataDTO) dto;
        dollarDto.setInvers(this.invers.get());
        dollarDto.setAllUnLock(this.allUnLock.get());
        return dto;
    }
}
