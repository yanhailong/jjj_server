package com.jjg.game.core.data;

/**
 * 道具
 * @author 11
 * @date 2025/8/7 15:10
 */
public class Item {
    //道具id
    private int id;
    //道具数量
    private long count;

    public Item() {
    }

    public Item(int id, long count) {
        this.id = id;
        this.count = count;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void addCount(long count) {
        this.count += count;
    }
}
