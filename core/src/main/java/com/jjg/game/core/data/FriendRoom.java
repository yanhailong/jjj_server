package com.jjg.game.core.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(FriendRoom.class);
    @Id
    protected long id;
    // 房间过期时间
    protected long overdueTime;
    // 房间名
    protected String aliasName;
    // 是否开启自动续费
    protected boolean autoRenewal;
    // 开启自动续费时
    protected int roomExpendId;
    // 庄家的预付金币
    protected long predictCostGoldNum;
    // 房间状态 1. 运行中 2. 暂停中 3. 解散中
    protected int status;
    // 房间暂停时间，开启时需要置为0
    protected long pauseTime;
    // 申请庄家的预付金币
    protected LinkedHashMap<Long, Long> bankerPredicateMap = new LinkedHashMap<>();
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
    @Override
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
     * 获取申请庄家玩家的金币
     */
    public long getApplyBankerPlayerGold(long playerId) {
        return bankerPredicateMap.get(playerId);
    }


    /**
     * 取消申请成为庄家
     */
    public long cancelApplyBanker(long playerId) {
        return bankerPredicateMap.remove(playerId);
    }

    /**
     * 添加预付金币
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

    public long getCreatorIncome() {
        return creatorIncome;
    }

    public void addCreatorIncome(long creatorIncome) {
        this.creatorIncome += creatorIncome;
    }

    public LinkedHashMap<Long, Long> getBankerPredicateMap() {
        return bankerPredicateMap;
    }

    /**
     * 减少准备金
     */
    @Override
    public void deductBankerGold(long deductGold) {
        // 优先扣除庄家的
        if (deductGold < 0) {
            return;
        }
        Map.Entry<Long, Long> bankerInf = bankerPredicateMap.firstEntry();
        long deductRes = bankerInf.getValue() - deductGold;
        // 需要继续扣除底庄的金币
        if (deductRes < 0) {
            long predicateCostRes = predictCostGoldNum - deductRes;
            if (predicateCostRes < 0) {
                predictCostGoldNum = 0;
                log.error("扣除准备金币失败，扣除值：{}，庄家值：{} 底庄值：{}", deductGold, bankerInf.getValue(), predictCostGoldNum);
                return;
            } else {
                predictCostGoldNum = predicateCostRes;
            }
            // 设置扣除后的值
            bankerPredicateMap.put(bankerInf.getKey(), 0L);
        } else {
            // 设置扣除后的值
            bankerPredicateMap.put(bankerInf.getKey(), deductRes);
        }
    }

    /**
     * 房间所有的预付金币
     */
    @Override
    public long bankerTotalGold() {
        return this.bankerPredicateMap.values().stream().mapToLong(a -> a).sum() + predictCostGoldNum;
    }

    public int getRoomExpendId() {
        return roomExpendId;
    }

    public void setRoomExpendId(int roomExpendId) {
        this.roomExpendId = roomExpendId;
    }

    @Override
    public String toString() {
        return "FriendRoom{" +
            "id=" + id +
            ", overdueTime=" + overdueTime +
            ", aliasName='" + aliasName + '\'' +
            ", autoRenewal=" + autoRenewal +
            ", roomExpendId=" + roomExpendId +
            ", predictCostGoldNum=" + predictCostGoldNum +
            ", type=" + type +
            ", gameType=" + gameType +
            ", roomCfgId=" + roomCfgId +
            ", path='" + path + '\'' +
            ", createTime=" + createTime +
            ", creator=" + creator +
            '}';
    }
}
