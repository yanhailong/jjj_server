package com.jjg.game.room.data.room;

import com.jjg.game.core.data.Player;

/**
 * 玩家游戏中的数据集
 *
 * @author 2CL
 */
public class GamePlayer extends Player {
    // 玩家是否处于托管状态
    protected boolean hosting;
    // table类的玩家数据
    protected TablePlayerGameData tableGameData;

    public boolean isHosting() {
        return hosting;
    }

    public void setHosting(boolean hosting) {
        this.hosting = hosting;
    }

    public TablePlayerGameData getTableGameData() {
        return tableGameData;
    }

    public void setTableGameData(TablePlayerGameData tableGameData) {
        this.tableGameData = tableGameData;
    }
}
