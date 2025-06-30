package com.jjg.game.slots.game.dollarexpress.data;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/17 14:25
 */
public class AwardLineInfo {
    private int id;
    private int times;
    private List<Integer> indexList;

    public AwardLineInfo() {
    }

    public AwardLineInfo(int id, int times) {
        this.id = id;
        this.times = times;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public List<Integer> getIndexList() {
        return indexList;
    }

    public void setIndexList(List<Integer> indexList) {
        this.indexList = indexList;
    }
}
