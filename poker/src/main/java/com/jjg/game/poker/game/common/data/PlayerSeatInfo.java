package com.jjg.game.poker.game.common.data;

import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.TimerEvent;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/28 11:25
 */
public class PlayerSeatInfo {
    /**
     * 座位id
     */
    private int seatId;
    /**
     * 玩家id
     */
    private long playerId;
    /**
     * 是否已经结束本轮游戏
     */
    private boolean isOver;
    /**
     * 当前轮次操作类型
     */
    private int operationType;
    /**
     * 手牌
     */
    private List<List<Integer>> cards;
    /**
     * 手牌索引
     */
    private int cardIndex;


    public PlayerSeatInfo() {
    }

    public PlayerSeatInfo(int seatId, long playerId) {
        this.seatId = seatId;
        this.playerId = playerId;
    }

    public List<List<Integer>> getCards() {
        return cards;
    }

    public List<Integer> getCurrentCards() {
        return cards.get(cardIndex);
    }

    public int getCardIndex() {
        return cardIndex;
    }

    public void setCardIndex(int cardIndex) {
        this.cardIndex = cardIndex;
    }

    public void setCards(List<List<Integer>> cards) {
        this.cards = cards;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public boolean isOver() {
        return isOver;
    }

    public void setOver(boolean over) {
        isOver = over;
    }

    public int getOperationType() {
        return operationType;
    }

    public void setOperationType(int operationType) {
        this.operationType = operationType;
    }

}
