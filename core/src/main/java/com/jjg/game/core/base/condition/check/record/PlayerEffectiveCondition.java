package com.jjg.game.core.base.condition.check.record;

import java.util.List;

public class PlayerEffectiveCondition extends BaseCheckCondition {
    /**
     * 需要排除的游戏id warehouse.xlsx id
     */
    private List<Integer> exclusionGameIds;
    /**
     * 需要的游戏id warehouse.xlsx id
     */
    private List<Integer> gameIds;
    /**
     * 需要的游戏类型 warehouse.xlsx gameType
     */
    private List<Integer> gameType;
    /**
     * 需要的场次类型 warehouse.xlsx roomType
     */
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

