package com.jjg.game.room.data.room;

import com.jjg.game.core.data.Player;

import java.util.Objects;

/**
 * 玩家游戏中的数据集
 *
 * @author 2CL
 */
public class GamePlayer extends Player {
    // 玩家是否处于托管状态
    protected transient boolean hosting;
    // table类的玩家数据
    protected transient TablePlayerGameData tableGameData;
    // poker类的玩家数据
    protected transient PokerPlayerGameData pokerPlayerGameData;
    //进入游戏的时间
    protected int enterGameTime;


    public void setPokerPlayerGameData(PokerPlayerGameData pokerPlayerGameData) {
        this.pokerPlayerGameData = pokerPlayerGameData;
    }

    public boolean isHosting() {
        return hosting;
    }

    public void setHosting(boolean hosting) {
        this.hosting = hosting;
    }

    public TablePlayerGameData getTableGameData() {
        return tableGameData;
    }

    public PokerPlayerGameData getPokerPlayerGameData() {
        return pokerPlayerGameData;
    }

    public void setTableGameData(TablePlayerGameData tableGameData) {
        this.tableGameData = tableGameData;
    }

    public int getEnterGameTime() {
        return enterGameTime;
    }

    public void setEnterGameTime(int enterGameTime) {
        this.enterGameTime = enterGameTime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GamePlayer that = (GamePlayer) o;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }
}
