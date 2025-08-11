package com.jjg.game.room.data.room;

import com.jjg.game.core.data.Player;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.datatrack.RoomDataTrackLogger;

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

    @Override
    public void setGold(long gold) {
        super.setGold(gold);
    }
}
