package com.jjg.game.core.base.condition.check.record;

import java.util.List;

public class PlayerEffectiveDropCondition extends BaseCheckCondition {

    private List<Integer> exclusionGameIds;
    private List<Integer> gameIds;
    private List<Integer> gameType;
    private List<Integer> roomTypeIds;
    private int itemId;

    public List<Integer> getGameType() {
        return gameType;
    }

    public void setGameType(List<Integer> gameType) {
        this.gameType = gameType;
    }

    public List<Integer> getExclusionGameIds() {
        return exclusionGameIds;
    }

    public void setExclusionGameIds(List<Integer> exclusionGameIds) {
        this.exclusionGameIds = exclusionGameIds;
    }

    public List<Integer> getGameIds() {
        return gameIds;
    }

    public void setGameIds(List<Integer> gameIds) {
        this.gameIds = gameIds;
    }

    public List<Integer> getRoomTypeIds() {
        return roomTypeIds;
    }

    public void setRoomTypeIds(List<Integer> roomTypeIds) {
        this.roomTypeIds = roomTypeIds;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
}

