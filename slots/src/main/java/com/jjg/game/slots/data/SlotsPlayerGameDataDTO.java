package com.jjg.game.slots.data;

import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2025/8/5 14:11
 */
@Document
@CompoundIndex(name = "playerId_roomcfgid_unique_idx", def = "{'playerId': 1, 'roomCfgId': 1}", unique = true)
public class SlotsPlayerGameDataDTO {
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
    protected int lastModelId;
    //最近一次所在的区间
    protected int lastSectionIndex;
    //玩家累计押注金额
    protected long allBet;
    //玩家累计获得奖池(倍场)金额
    protected long rewardPoolGold;
    //玩家奖池(倍场)累计贡献金额金额(没有减去已获得金额)
    protected long contribtPoolGold;
    //免费游戏中累计获得的金币
    protected long freeAllWin;
    //剩余的免费次数
    protected int remainFreeCount;
    //当前的免费游戏数组中的下标值
    protected int freeIndex;

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

    public long getFreeAllWin() {
        return freeAllWin;
    }

    public void setFreeAllWin(long freeAllWin) {
        this.freeAllWin = freeAllWin;
    }

    public int getRemainFreeCount() {
        return remainFreeCount;
    }

    public void setRemainFreeCount(int remainFreeCount) {
        this.remainFreeCount = remainFreeCount;
    }

    public int getFreeIndex() {
        return freeIndex;
    }

    public void setFreeIndex(int freeIndex) {
        this.freeIndex = freeIndex;
    }

    public <T extends SlotsPlayerGameData> T converToGameData(Class<T> cla) throws Exception{
        Constructor<T> constructor = cla.getConstructor();
        T t = constructor.newInstance();
        BeanUtils.copyProperties(this,t);
        t.setRemainFreeCount(new AtomicInteger(this.remainFreeCount));
        t.setFreeIndex(new AtomicInteger(this.freeIndex));
        return t;
    }
}
