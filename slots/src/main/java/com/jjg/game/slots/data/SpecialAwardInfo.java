package com.jjg.game.slots.data;

/**
 * 特殊图标奖励
 * @author 11
 * @date 2025/7/2 17:02
 */
public class SpecialAwardInfo {
    private int count;
    private int times;

    public SpecialAwardInfo() {
    }

    public SpecialAwardInfo(int count, int times) {
        this.count = count;
        this.times = times;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }
}
