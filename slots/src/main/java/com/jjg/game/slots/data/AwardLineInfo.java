package com.jjg.game.slots.data;


/**
 * 中奖线信息
 * @author 11
 * @date 2025/6/17 14:25
 */
public class AwardLineInfo {
    //线的id
    protected int id;
    //这条线的基础倍数
    protected int baseTimes;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }
}
