package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.beans.BeanUtils;
import org.springframework.data.annotation.Transient;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 玩家游戏数据
 * @author 11
 * @date 2025/6/10 18:07
 */
public class DollarExpressPlayerGameData extends SlotsPlayerGameData {
    //最近一次的押注(总押分)
    private long lastBet;
    //最近一次的模式id
    private int lastModelId;
    //最近一次所在的区间
    private int lastSectionIndex;
    //玩家累计押注金额
    private long allBet;
    //玩家累计获得奖池(倍场)金额
    private Map<Integer,Long> rewardPoolGoldMap;
    //玩家奖池(倍场)累计贡献金额金额(没有减去已获得金额)
    private Map<Integer,Long> contribtPoolGoldMap;
    //累计的美钞数量
    private int totalDollars;
    //记录出现可收集美元的局数
    private int addDollarsCount;
    //记录收集美元时的押注总和(用于计算累计的美钞的平均值)
    private long addDollarsTotalStake;

    //用于测试
    @Transient
    private LinkedList<TestLibData> testLibDataList;

    //剩余的免费次数
    private AtomicInteger remainFreeCount = new AtomicInteger(0);
    //是否可以投资
    private AtomicBoolean invers = new AtomicBoolean(false);
    //已经选择的地区
    private Set<Integer> selectedAreaSet;
    //全地图解锁
    private AtomicBoolean allUnLock = new AtomicBoolean(false);

    public long getLastBet() {
        return lastBet;
    }

    public void setLastBet(long lastBet) {
        this.lastBet = lastBet;
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

    public int getLastModelId() {
        return lastModelId;
    }

    public void setLastModelId(int lastModelId) {
        this.lastModelId = lastModelId;
    }

    public int getLastSectionIndex() {
        return lastSectionIndex;
    }

    public void setLastSectionIndex(int lastSectionIndex) {
        this.lastSectionIndex = lastSectionIndex;
    }

    public AtomicInteger getRemainFreeCount() {
        return remainFreeCount;
    }

    public long getAllBet() {
        return allBet;
    }

    public void addAllBet(long bet) {
        this.allBet += bet;
    }

    public void setAllBet(long allBet) {
        this.allBet = allBet;
    }

    public Map<Integer, Long> getRewardPoolGoldMap() {
        return rewardPoolGoldMap;
    }

    public void setRewardPoolGoldMap(Map<Integer, Long> rewardPoolGoldMap) {
        this.rewardPoolGoldMap = rewardPoolGoldMap;
    }

    public Map<Integer, Long> getContribtPoolGoldMap() {
        return contribtPoolGoldMap;
    }

    public void setContribtPoolGoldMap(Map<Integer, Long> contribtPoolGoldMap) {
        this.contribtPoolGoldMap = contribtPoolGoldMap;
    }

    public void setRemainFreeCount(AtomicInteger remainFreeCount) {
        this.remainFreeCount = remainFreeCount;
    }

    /**
     * 获取玩家对奖池的累计贡献金额
     * @return
     */
    public long getAllContribtPoolGold() {
        if(this.contribtPoolGoldMap == null || this.contribtPoolGoldMap.isEmpty()){
            return 0;
        }

        //总累计
        Long contribtGold = this.contribtPoolGoldMap.get(this.roomCfgId);
        if(contribtGold == null){
            return 0;
        }

        if(this.rewardPoolGoldMap == null || this.rewardPoolGoldMap.isEmpty()){
            return contribtGold;
        }
        //总获得
        Long rewardGold = this.rewardPoolGoldMap.get(this.roomCfgId);
        if(rewardGold == null){
            return contribtGold;
        }
        return contribtGold - rewardGold;
    }

    public long addContribtPoolGold(long value){
        if(this.contribtPoolGoldMap == null){
            this.contribtPoolGoldMap = new HashMap<>();
        }
        return this.contribtPoolGoldMap.merge(this.roomCfgId, value, Long::sum);
    }

    public void addTestIconsData(TestLibData testLibData) {
        if(this.testLibDataList == null){
            this.testLibDataList = new LinkedList<>();
        }
        this.testLibDataList.add(testLibData);
    }

    public TestLibData pollTestLibData() {
        if(this.testLibDataList == null || this.testLibDataList.isEmpty()){
            return null;
        }
        return this.testLibDataList.poll();
    }

    public AtomicBoolean getInvers() {
        return invers;
    }

    public LinkedList<TestLibData> getTestLibDataList() {
        return testLibDataList;
    }

    public void setTestLibDataList(LinkedList<TestLibData> testLibDataList) {
        this.testLibDataList = testLibDataList;
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

    public long addSmallPoolReward(long gold){
        if(this.rewardPoolGoldMap == null){
            this.rewardPoolGoldMap = new HashMap<>();
        }
        return this.rewardPoolGoldMap.merge(this.roomCfgId, gold, Long::sum);
    }



    public DollarExpressPlayerGameDataDTO converToDto(){
        DollarExpressPlayerGameDataDTO dto = new DollarExpressPlayerGameDataDTO();
        BeanUtils.copyProperties(this,dto);
        return dto;
    }
}
