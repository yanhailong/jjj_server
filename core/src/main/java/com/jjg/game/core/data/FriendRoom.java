package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 匹配房间，需要存库，因为玩家可以暂停房间中的时间，而且后续还可以恢复房间，所以需要在玩家暂停一定时间之后直接存库，
 * 然后销毁内存和redis中的房间数据，当玩家再次运行房间时需要恢复房间中的状态
 *
 * @author 2CL
 */
public class FriendRoom extends Room {
    @Id
    protected long id;
    // 房间过期时间
    protected long overdueTime;
    // 房间名
    protected String aliasName;
    // 是否开启自动续费
    protected boolean autoRenewal;
    // 庄家的预付金
    protected long predictCostGoldNum;
    // 房间状态 1. 运行中 2. 暂停中 3. 解散中
    protected int status;
    // 房间暂停时间，开启时需要置为0
    protected long pauseTime;
    // 总流水
    protected long totalFlowing;
    // 申请庄家的预付金
    protected LinkedHashMap<Long, Long> bankerPredicateMap = new LinkedHashMap<>();
    // 每个玩家的收益流水
    protected Map<Long, Long> playerIncomeRec = new HashMap<>();
    // 房间创建者收益
    protected long creatorIncome;

    /**
     * 场上是否有庄家
     */
    public boolean hasBanker() {
        return !bankerPredicateMap.isEmpty();
    }

    /**
     * 房间庄家ID
     */
    public long roomBankerId() {
        if (bankerPredicateMap.isEmpty()) {
            return 0L;
        }
        return bankerPredicateMap.firstEntry().getKey();
    }

    /**
     * 房间庄家剩余金币
     */
    public long roomBankerResetGold() {
        if (bankerPredicateMap.isEmpty()) {
            return 0L;
        }
        return bankerPredicateMap.firstEntry().getValue();
    }

    /**
     * 移除庄家
     *
     * @return 剩余的准备金
     */
    public Map.Entry<Long, Long> removeBanker() {
        return bankerPredicateMap.pollFirstEntry();
    }

    /**
     * 添加预付金
     */
    public void addBankerSupply(long bankerId, long predictCostGoldNum) {
        bankerPredicateMap.put(bankerId,
            bankerPredicateMap.getOrDefault(bankerId, 0L) + predictCostGoldNum);
    }

    public long getOverdueTime() {
        return overdueTime;
    }

    public void setOverdueTime(long overdueTime) {
        this.overdueTime = overdueTime;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public boolean isAutoRenewal() {
        return autoRenewal;
    }

    public void setAutoRenewal(boolean autoRenewal) {
        this.autoRenewal = autoRenewal;
    }

    public long getPredictCostGoldNum() {
        return predictCostGoldNum;
    }

    public void setPredictCostGoldNum(long predictCostGoldNum) {
        this.predictCostGoldNum = predictCostGoldNum;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getPauseTime() {
        return pauseTime;
    }

    public void setPauseTime(long pauseTime) {
        this.pauseTime = pauseTime;
    }

    public Map<Long, Long> getPlayerIncomeRec() {
        return playerIncomeRec;
    }

    public void setPlayerIncomeRec(Map<Long, Long> playerIncomeRec) {
        this.playerIncomeRec = playerIncomeRec;
    }

    public long getTotalFlowing() {
        return totalFlowing;
    }

    public void setTotalFlowing(long totalFlowing) {
        this.totalFlowing = totalFlowing;
    }

    public long getCreatorIncome() {
        return creatorIncome;
    }

    public void setCreatorIncome(long creatorIncome) {
        this.creatorIncome = creatorIncome;
    }

    public LinkedHashMap<Long, Long> getBankerPredicateMap() {
        return bankerPredicateMap;
    }

    public void setBankerPredicateMap(LinkedHashMap<Long, Long> bankerPredicateMap) {
        this.bankerPredicateMap = bankerPredicateMap;
    }
}
