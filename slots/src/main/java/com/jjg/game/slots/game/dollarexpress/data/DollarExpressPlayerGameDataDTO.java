package com.jjg.game.slots.game.dollarexpress.data;

import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2025/8/5 14:10
 */
@Document
public class DollarExpressPlayerGameDataDTO extends SlotsPlayerGameDataDTO {
    //最近一次的押注(除了100)
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
    private int addDollarsTotalStake;

    //剩余的免费次数
    private int remainFreeCount;
    //是否可以投资
    private boolean invers;
    //已经选择的地区
    private Set<Integer> selectedAreaSet;
    //全地图解锁
    private boolean allUnLock;

    public long getLastBet() {
        return lastBet;
    }

    public void setLastBet(long lastBet) {
        this.lastBet = lastBet;
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

    public long getAllBet() {
        return allBet;
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

    public int getTotalDollars() {
        return totalDollars;
    }

    public void setTotalDollars(int totalDollars) {
        this.totalDollars = totalDollars;
    }

    public int getAddDollarsCount() {
        return addDollarsCount;
    }

    public void setAddDollarsCount(int addDollarsCount) {
        this.addDollarsCount = addDollarsCount;
    }

    public int getAddDollarsTotalStake() {
        return addDollarsTotalStake;
    }

    public void setAddDollarsTotalStake(int addDollarsTotalStake) {
        this.addDollarsTotalStake = addDollarsTotalStake;
    }

    public int getRemainFreeCount() {
        return remainFreeCount;
    }

    public void setRemainFreeCount(int remainFreeCount) {
        this.remainFreeCount = remainFreeCount;
    }

    public boolean isInvers() {
        return invers;
    }

    public void setInvers(boolean invers) {
        this.invers = invers;
    }

    public Set<Integer> getSelectedAreaSet() {
        return selectedAreaSet;
    }

    public void setSelectedAreaSet(Set<Integer> selectedAreaSet) {
        this.selectedAreaSet = selectedAreaSet;
    }

    public boolean isAllUnLock() {
        return allUnLock;
    }

    public void setAllUnLock(boolean allUnLock) {
        this.allUnLock = allUnLock;
    }
}
