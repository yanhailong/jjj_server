package com.jjg.game.slots.game.garaGemstone1.data;

/**
 * 伽罗宝石 倍数轴配置信息
 * 对应 SpecialPlay.xlsx 5055011 中每一个条目
 * 格式：iconId_times_weight
 */
public class GaraGemstone1MultiplyAxisInfo {

    /** 符号ID（需在 BaseRoller 20550114 elements 中存在） */
    private int iconId;
    /** 倍数值（1/2/3/5/10/15），奖金符号填0 */
    private int times;
    /** 权重 */
    private int weight;

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
}
