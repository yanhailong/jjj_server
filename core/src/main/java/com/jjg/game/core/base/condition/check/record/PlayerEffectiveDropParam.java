package com.jjg.game.core.base.condition.check.record;

import java.util.List;

public class PlayerEffectiveDropParam extends BaseCheckParam {
    private int gameId;
    private int roomType;
    private int gameType;
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
