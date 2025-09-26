package com.jjg.game.poker.game.texas.data;

/**
 * 座位信息
 * @author lm
 * @date 2025/7/31 14:48
 */
public class SeatInfo {
    //玩家id
    private long playerId;
    //是否坐下
    private boolean seatDown;
    //是否参与游戏
    private boolean joinGame;
    //是否参与准备
    private boolean ready;
    //座位id
    private int seatId;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public boolean isSeatDown() {
        return seatDown;
    }

    public void setSeatDown(boolean seatDown) {
        this.seatDown = seatDown;
    }

    public boolean isJoinGame() {
        return joinGame;
    }

    public void setJoinGame(boolean joinGame) {
        this.joinGame = joinGame;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
