package com.jjg.game.dollarexpress.data;

/**
 * @author 11
 * @date 2025/6/17 14:25
 */
public class AwardLineInfo {
    private int id;
    private int times;

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
}
