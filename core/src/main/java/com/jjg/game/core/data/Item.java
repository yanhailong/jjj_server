package com.jjg.game.core.data;

/**
 * 道具
 *
 * @author 11
 * @date 2025/8/7 15:10
 */
public class Item {
    //道具id
    private int itemId;
    //道具数量
    private long itemCount;
    //格子ID
    private Integer gridId = null;

    public Item() {
    }

    public Item(int itemId, long itemCount) {
        this.itemId = itemId;
        this.itemCount = itemCount;
    }

    public Item(int gridId, int itemId, long itemCount) {
        this.itemId = itemId;
        this.itemCount = itemCount;
        this.gridId = gridId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public long getItemCount() {
        return itemCount;
    }

    public void setItemCount(long itemCount) {
        this.itemCount = itemCount;
    }

    public void addCount(long count) {
        this.itemCount += count;
    }

    public Integer getGridId() {
        return gridId;
    }

    public void setGridId(int gridId) {
        this.gridId = gridId;
    }
}
