package com.jjg.game.slots.game.garaGemstone1.data;

/**
 * 伽罗宝石 倍数轴配置信息
 * 对应 SpecialPlay.xlsx 5055011 中每一个条目
 */
public class GaraGemstone1MultiplyAxisInfo {

    /** 符号ID */
    private int iconId;
    /** 权重 */
    private int weight;
    /** 倍数值，0 表示奖金符号（bonus），触发对应奖池 */
    private int times;
    /** 奖池ID，大于0时表示奖金符号，触发对应奖池奖励 */
    private int poolId;

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public int getPoolId() {
        return poolId;
    }

    public void setPoolId(int poolId) {
        this.poolId = poolId;
    }
}
