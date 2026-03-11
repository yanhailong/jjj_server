package com.jjg.game.room.data.room;

import com.jjg.game.common.proto.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * table类的玩家数据保存
 *
 * @author 2CL
 */
public class TablePlayerGameData {

    // 最近20场的下注数据 / 临时数据
    private final List<Pair<Boolean, Long>> betInfoList = new ArrayList<>();
    // 红黑大战玩家本场此押注总金币
    private long totalBet;
    // 玩家上一次在房间主动操作并与服务端有交互的时间
    private long playerLatestOperateTime;
    // 玩家上一次在房间主动操作并与服务端有交互的发送标记，防止重发
    private boolean hasNotifyNoOperate = false;
    // 每轮开始时的玩家数据，仅做记录使用
    private long roundStartPlayerGold;
    // 玩家 玩了 多少把 （俄罗斯转盘）
    private int playNum;

    public void addBetRecord(long getGold) {
        int i = (betInfoList.size() - 20) + 1;
        if (i > 0) {
            betInfoList.subList(0, i).clear();
        }
        betInfoList.add(Pair.newPair(getGold > 0, totalBet));
        totalBet = 0;
    }

    public List<Pair<Boolean, Long>> getBetInfoList() {
        return betInfoList;
    }

    public long getTotalBet() {
        return totalBet;
    }

    public void addTotalBet(long bet) {
        this.totalBet += bet;
    }

    public long getPlayerLatestOperateTime() {
        return playerLatestOperateTime;
    }

    public void setPlayerLatestOperateTime(long playerLatestOperateTime) {
        this.playerLatestOperateTime = playerLatestOperateTime;
    }

    public boolean isHasNotifyNoOperate() {
        return hasNotifyNoOperate;
    }

    public void setHasNotifyNoOperate(boolean hasNotifyNoOperate) {
        this.hasNotifyNoOperate = hasNotifyNoOperate;
    }

    public long getRoundStartPlayerGold() {
        return roundStartPlayerGold;
    }

    public void setRoundStartPlayerGold(long roundStartPlayerGold) {
        this.roundStartPlayerGold = roundStartPlayerGold;
    }

    public int getPlayNum() {
        return playNum;
    }

    public void setPlayNum(int playNum) {
        this.playNum = playNum;
    }

    public void addPlayNum() {
        this.playNum = this.playNum + 1;
    }
}
