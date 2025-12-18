package com.jjg.game.core.data;

public class FriendRoomRewardItem {
    private long id;
    private int gameMajorType;
    private int itemId;
    private long count;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getGameMajorType() {
        return gameMajorType;
    }

    public void setGameMajorType(int gameMajorType) {
        this.gameMajorType = gameMajorType;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
