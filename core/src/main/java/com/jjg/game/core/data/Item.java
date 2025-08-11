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
    private int count;

    public Item() {
    }

    public Item(int id, int count) {
        this.id = id;
        this.count = count;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void addCount(int count) {
        this.count += count;
    }
}
