package com.jjg.game.slots.data;

import org.springframework.data.annotation.Id;

/**
 * @author 11
 * @date 2025/8/5 14:11
 */
public class SlotsPlayerGameDataDTO {
    @Id
    protected long playerId;
    //游戏类型
    protected int gameType;
    //场次配置id
    protected int roomCfgId;
    //当前所处状态(美元快递) 0.正常  1.二选一  2.正在免费旋转
    protected int status;
    //单线押分
    protected long oneBetScore;
    //总押分
    protected long allBetScore;
    //最近一次的模式id
    private int lastModelId;
    //最近一次所在的区间
    private int lastSectionIndex;
    //玩家累计押注金额
    private long allBet;
    //玩家累计获得奖池(倍场)金额
    private long rewardPoolGold;
    //玩家奖池(倍场)累计贡献金额金额(没有减去已获得金额)
    private long contribtPoolGold;
    //剩余的免费次数
    private int remainFreeCount;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getOneBetScore() {
        return oneBetScore;
    }

    public void setOneBetScore(long oneBetScore) {
        this.oneBetScore = oneBetScore;
    }

    public long getAllBetScore() {
        return allBetScore;
    }

    public void setAllBetScore(long allBetScore) {
        this.allBetScore = allBetScore;
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

    public long getRewardPoolGold() {
        return rewardPoolGold;
    }

    public void setRewardPoolGold(long rewardPoolGold) {
        this.rewardPoolGold = rewardPoolGold;
    }

    public long getContribtPoolGold() {
        return contribtPoolGold;
    }

    public void setContribtPoolGold(long contribtPoolGold) {
        this.contribtPoolGold = contribtPoolGold;
    }

    public int getRemainFreeCount() {
        return remainFreeCount;
    }

    public void setRemainFreeCount(int remainFreeCount) {
        this.remainFreeCount = remainFreeCount;
    }
}
