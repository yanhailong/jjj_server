package com.jjg.game.slots.data;

import com.jjg.game.core.constant.Code;

/**
 * @author 11
 * @date 2025/6/12 17:38
 */
public class GameRunInfo<T extends SlotsPlayerGameData> {
    protected int code;
    protected long playerId;
    //当前所处的状态
    protected int status;
    //是否为系统自动
    protected boolean auto;
    //标准池子中奖倍数
    private long bigPoolTimes;
    //玩家之前的金币
    private long beforeGold;
    //总计获得的金币
    private long allWinGold;
    //总计奖池获得金额
    private long smallPoolGold;
    //玩家当前的金币
    private long afterGold;
    //总押分
    private long stake;
    //图标
    private int[] iconArr;
    //大奖展示id
    private int bigShowId;
    //剩余免费次数
    private int remainFreeCount;
    //玩家游戏数据
    private T data;
    //结果库
    private Object resultLib;

    public GameRunInfo(int code, long playerId) {
        this.code = code;
        this.playerId = playerId;
    }

    public boolean success() {
        return this.code == Code.SUCCESS;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isAuto() {
        return auto;
    }

    public void setAuto(boolean auto) {
        this.auto = auto;
    }

    public long getAllWinGold() {
        return allWinGold;
    }

    public void setAllWinGold(long allWinGold) {
        this.allWinGold = allWinGold;
    }

    public long getBigPoolTimes() {
        return bigPoolTimes;
    }

    public void setBigPoolTimes(long bigPoolTimes) {
        this.bigPoolTimes = bigPoolTimes;
    }

    public void addBigPoolTimes(long allTimes) {
        this.bigPoolTimes += allTimes;
    }

    public int[] getIconArr() {
        return iconArr;
    }

    public void setIconArr(int[] iconArr) {
        this.iconArr = iconArr;
    }

    public long getStake() {
        return stake;
    }

    public void setStake(long stake) {
        this.stake = stake;
    }

    public void addAllWinGold(long winGold) {
        this.allWinGold += winGold;
    }

    public long getBeforeGold() {
        return beforeGold;
    }

    public void setBeforeGold(long beforeGold) {
        this.beforeGold = beforeGold;
    }

    public long getAfterGold() {
        return afterGold;
    }

    public void setAfterGold(long afterGold) {
        this.afterGold = afterGold;
    }

    public int getBigShowId() {
        return bigShowId;
    }

    public void setBigShowId(int bigShowId) {
        this.bigShowId = bigShowId;
    }

    public long getSmallPoolGold() {
        return smallPoolGold;
    }

    public void setSmallPoolGold(long smallPoolGold) {
        this.smallPoolGold = smallPoolGold;
    }

    public void addSmallPoolGold(long smallPoolGold) {
        this.smallPoolGold += smallPoolGold;
    }

    public int getRemainFreeCount() {
        return remainFreeCount;
    }

    public void setRemainFreeCount(int remainFreeCount) {
        this.remainFreeCount = remainFreeCount;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Object getResultLib() {
        return resultLib;
    }

    public void setResultLib(Object resultLib) {
        this.resultLib = resultLib;
    }
}
