package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * 好友房历史账单bean
 *
 * @author 2CL
 */
@Document
public class FriendRoomBillHistoryBean {
    @Id
    private long id;
    // 游戏类型
    private int gameType;
    // 游戏配置ID
    private int gameCfgId;
    // 房间ID
    private long roomId;
    // 房间创建者
    private long roomCreator;
    // 总流水
    private long totalFlowing;
    // 总收益
    private long totalIncome;
    // 是否已经领取了收益
    private boolean hasTookIncome;
    // 账单创建时间
    private long createdAt;
    // 参与玩家收益 玩家ID + 收益数量
    private Map<Long, Long> partInPlayerIncome;
    // 道具ID
    private int itemId;
    // 月份
    private int month;
    // 玩家总押分 玩家ID + 总押分
    private Map<Long, Long> partInPlayerBetScore;
    //游戏主类型
    private int gameMajorType;
    //已经领取的收益额
    private long hasReceivedIncome;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTotalFlowing() {
        return totalFlowing;
    }

    public void setTotalFlowing(long totalFlowing) {
        this.totalFlowing = totalFlowing;
    }

    public long getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(long totalIncome) {
        this.totalIncome = totalIncome;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isHasTookIncome() {
        return hasTookIncome;
    }

    public void setHasTookIncome(boolean hasTookIncome) {
        this.hasTookIncome = hasTookIncome;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public long getRoomCreator() {
        return roomCreator;
    }

    public void setRoomCreator(long roomCreator) {
        this.roomCreator = roomCreator;
    }

    public int getGameCfgId() {
        return gameCfgId;
    }

    public void setGameCfgId(int gameCfgId) {
        this.gameCfgId = gameCfgId;
    }

    public Map<Long, Long> getPartInPlayerIncome() {
        return partInPlayerIncome;
    }

    public void setPartInPlayerIncome(Map<Long, Long> partInPlayerIncome) {
        this.partInPlayerIncome = partInPlayerIncome;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public Map<Long, Long> getPartInPlayerBetScore() {
        return partInPlayerBetScore;
    }

    public void setPartInPlayerBetScore(Map<Long, Long> partInPlayerBetScore) {
        this.partInPlayerBetScore = partInPlayerBetScore;
    }

    public int getGameMajorType() {
        return gameMajorType;
    }

    public void setGameMajorType(int gameMajorType) {
        this.gameMajorType = gameMajorType;
    }

    public long getHasReceivedIncome() {
        return hasReceivedIncome;
    }

    public void setHasReceivedIncome(long hasReceivedIncome) {
        this.hasReceivedIncome = hasReceivedIncome;
    }

    public Long getPlayerInCome(long playerId){
        if(this.partInPlayerIncome == null || this.partInPlayerIncome.isEmpty()){
            return null;
        }
        return  this.partInPlayerIncome.get(playerId);
    }
}
