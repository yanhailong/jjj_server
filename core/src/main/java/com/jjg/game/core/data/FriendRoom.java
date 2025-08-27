package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    // 预付金
    protected long predictCostGoldNum;
    // 房间状态 1. 运行中 2. 暂停中 3. 解散中
    protected int status;
    // 房间暂停时间，开启时需要置为0
    protected long pauseTime;
    // 总流水
    protected long totalFlowing;
    // 每个玩家的收益流水
    protected Map<Long, Long> playerIncomeRec;
    // 房间创建者收益
    protected long creatorIncome;

    // 房间的庄家ID
    private long bankerId;

    public long getBankerId() {
        return bankerId;
    }

    public void setBankerId(long bankerId) {
        this.bankerId = bankerId;
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
}
