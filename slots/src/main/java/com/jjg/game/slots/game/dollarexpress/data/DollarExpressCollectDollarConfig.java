package com.jjg.game.slots.game.dollarexpress.data;

/**
 * 收集美元的一些配置
 * @author 11
 * @date 2025/7/18 19:12
 */
public class DollarExpressCollectDollarConfig {
    //单线押分(低于此值不开启收集功能)
    private long stakeMin;
    //起始值
    private int begin;
    //收集概率
    private int prop;
    //收集的目标个数
    private int max;
    //游戏id
    private int auxiliaryId;

    public long getStakeMin() {
        return stakeMin;
    }

    public void setStakeMin(long stakeMin) {
        this.stakeMin = stakeMin;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getProp() {
        return prop;
    }

    public void setProp(int prop) {
        this.prop = prop;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getAuxiliaryId() {
        return auxiliaryId;
    }

    public void setAuxiliaryId(int auxiliaryId) {
        this.auxiliaryId = auxiliaryId;
    }
}
