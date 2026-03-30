package com.jjg.game.slots.game.steamAge.data;

/**
 * @author lihaocao
 * @date 2025/9/10 9:42
 */

//中奖次数，就是触发中奖条件
//新增周ID，就是轴内容
//中奖权重，就是根据此权重判断是否中奖，返回中奖or不中奖，如果根据轴的内容不符合中奖权重结果，则抛弃当前结果重新随机轴图案，抛弃100次后仍然不符合则适用当前结果
//中奖倍率，中奖时额外的倍率
public class SteamAgeExpandRollerInfo {

    //类型 1 正常 2 免费
    private int libType;
    //中奖次数
    private int winTimes;
    //轴内容
    private int rollerId;
    //权重
    private int weight;
    //中奖倍数
    private int baseTimes;

    public SteamAgeExpandRollerInfo() {
    }

    public int getLibType() {
        return libType;
    }

    public void setLibType(int libType) {
        this.libType = libType;
    }

    public int getWinTimes() {
        return winTimes;
    }

    public void setWinTimes(int winTimes) {
        this.winTimes = winTimes;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getRollerId() {
        return rollerId;
    }

    public void setRollerId(int rollerId) {
        this.rollerId = rollerId;
    }

    public int getBaseTimes() {
        return baseTimes;
    }

    public void setBaseTimes(int baseTimes) {
        this.baseTimes = baseTimes;
    }
}
