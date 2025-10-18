package com.jjg.game.core.base.condition.check.record;

import java.util.List;

public class PlayerEffectiveParam extends BaseCheckParam {
    /**
     * 游戏id warehouse.xlsx id
     */
    private int gameId;
    /**
     * 游戏房间类型 warehouse.xlsx roomType
     */
    private int roomType;
    /**
     * 游戏大类型 warehouse.xlsx gameType
     */
    private int gameType;
    /**
     * 附加参数 根据不同需要按顺序放入，具体看使用类
     */
    private List<Long> paramList;

    public int getRoomType() {
        return roomType;
    }

    public void setRoomType(int roomType) {
        this.roomType = roomType;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
    public List<Long> getParamList() {
        return paramList;
    }

    public void setParamList(List<Long> paramList) {
        this.paramList = paramList;
    }
}
