package com.jjg.game.core.base.condition.event;

/**
 * @author lm
 * @date 2026/1/14 15:53
 */
public class UserItemEvent {
    private int itemId;
    private long count;

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
