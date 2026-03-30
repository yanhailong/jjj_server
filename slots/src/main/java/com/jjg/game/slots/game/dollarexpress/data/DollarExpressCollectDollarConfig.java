package com.jjg.game.slots.game.dollarexpress.data;

import java.util.Map;

/**
 * 收集美元的一些配置
 * @author 11
 * @date 2025/7/18 19:12
 */
public class DollarExpressCollectDollarConfig {
    //单线押分(低于此值不开启收集功能)
    private long stakeMin;
    //总押分(低于此值不开启收集功能)
    private long stakeAllBetScoreMin;
    //起始值
    private int begin;
    //收集概率
    private int prop;
    //收集的目标个数
    private int max;
    //游戏id
    private int auxiliaryId;
    //触发标记
    private Map<Integer,Integer> triggerTarMap;
    //触发的类型
    private int triggerType;
    //全解锁奖励的游戏id
    private int triggerAuxiliaryId;
    //3次权中奖次数
    private int allWinCount;
    //3次全中奖后触发小游戏的概率
    private int allWinCountProp;
    //3次全中奖后触发的小游戏
    private int allWinCountAuxiliaryId;

    public long getStakeMin() {
        return stakeMin;
    }

    public void setStakeMin(long stakeMin) {
        this.stakeMin = stakeMin;
    }

    public int getBegin() {
        return begin;
    }

    public long getStakeAllBetScoreMin() {
        return stakeAllBetScoreMin;
    }

    public void setStakeAllBetScoreMin(long stakeAllBetScoreMin) {
        this.stakeAllBetScoreMin = stakeAllBetScoreMin;
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

    public Map<Integer, Integer> getTriggerTarMap() {
        return triggerTarMap;
    }

    public void setTriggerTarMap(Map<Integer, Integer> triggerTarMap) {
        this.triggerTarMap = triggerTarMap;
    }

    public int getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(int triggerType) {
        this.triggerType = triggerType;
    }

    public int getTriggerAuxiliaryId() {
        return triggerAuxiliaryId;
    }

    public void setTriggerAuxiliaryId(int triggerAuxiliaryId) {
        this.triggerAuxiliaryId = triggerAuxiliaryId;
    }

    public int getAllWinCount() {
        return allWinCount;
    }

    public void setAllWinCount(int allWinCount) {
        this.allWinCount = allWinCount;
    }

    public int getAllWinCountProp() {
        return allWinCountProp;
    }

    public void setAllWinCountProp(int allWinCountProp) {
        this.allWinCountProp = allWinCountProp;
    }

    public int getAllWinCountAuxiliaryId() {
        return allWinCountAuxiliaryId;
    }

    public void setAllWinCountAuxiliaryId(int allWinCountAuxiliaryId) {
        this.allWinCountAuxiliaryId = allWinCountAuxiliaryId;
    }
}
