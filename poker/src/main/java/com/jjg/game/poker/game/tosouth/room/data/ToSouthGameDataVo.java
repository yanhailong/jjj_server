package com.jjg.game.poker.game.tosouth.room.data;

import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.poker.game.tosouth.data.ToSouthDataHelper;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;

public class ToSouthGameDataVo extends BasePokerGameDataVo {
    private long roomBet;
    // 上一家出的牌
    private List<Integer> lastPlayCards;
    // 上一家座位
    private int lastPlaySeatId;
    // 当前轮的首出者
    private int roundLeaderSeatId;
    // 是否为第一轮
    private boolean isFirstRound;
    // 当前轮连续过牌次数
    private int passCount;
    // 本轮已过牌玩家列表
    private Set<Integer> curRoundPassedPlayerSeats = new HashSet<>();
    // 当前回合出牌记录 (用于炸弹结算)
    private final List<ToSouthRoundRecord> currentRoundPlays = new ArrayList<>();
    // 炸弹结算积分变动 (playerId -> score change)
    private final Map<Long, Long> bombSettlementMap = new HashMap<>();

    /**
     * 必须初始化的参数是房间配置RoomCfg，如果后续子类添加数据需要在自己的构造函数中添加
     *
     * @param roomCfg
     */
    public ToSouthGameDataVo(Room_ChessCfg roomCfg) {
        super(roomCfg);
    }

    public long getRoomBet() {
        return roomBet;
    }

    public void setRoomBet(long roomBet) {
        this.roomBet = roomBet;
    }

    public List<Integer> getLastPlayCards() {
        return lastPlayCards;
    }

    public void setLastPlayCards(List<Integer> lastPlayCards) {
        this.lastPlayCards = lastPlayCards;
    }

    public int getLastPlaySeatId() {
        return lastPlaySeatId;
    }

    public void setLastPlaySeatId(int lastPlaySeatId) {
        this.lastPlaySeatId = lastPlaySeatId;
    }

    public int getRoundLeaderSeatId() {
        return roundLeaderSeatId;
    }

    public void setRoundLeaderSeatId(int roundLeaderSeatId) {
        this.roundLeaderSeatId = roundLeaderSeatId;
    }

    public boolean isFirstRound() {
        return isFirstRound;
    }

    public void setFirstRound(boolean firstRound) {
        isFirstRound = firstRound;
    }

    public int getPassCount() {
        return passCount;
    }

    public void setPassCount(int passCount) {
        this.passCount = passCount;
    }

    public Set<Integer> getCurRoundPassedPlayerSeats() {
        return curRoundPassedPlayerSeats;
    }

    public void setCurRoundPassedPlayerSeats(Set<Integer> curRoundPassedPlayerSeats) {
        this.curRoundPassedPlayerSeats = curRoundPassedPlayerSeats;
    }

    public List<ToSouthRoundRecord> getCurrentRoundPlays() {
        return currentRoundPlays;
    }

    public Map<Long, Long> getBombSettlementMap() {
        return bombSettlementMap;
    }

    @Override
    public int getPoolId() {
        return ToSouthDataHelper.getPoolId(this);
    }
}
