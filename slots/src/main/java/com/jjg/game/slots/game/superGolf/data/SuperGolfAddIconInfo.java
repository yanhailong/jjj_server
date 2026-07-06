package com.jjg.game.slots.game.superGolf.data;

import java.util.List;
import java.util.Map;

public class SuperGolfAddIconInfo {
    //添加的图标 坐标->图标id
    private Map<Integer, Integer> addIconMap;
    //本 cascade 的中奖信息
    private List<SuperGolfAwardLineInfo> awardLineInfoList;
    //本 cascade 触发的"神秘符号转换"：把带框中奖符号的位置变成 mystery（id=114）
    //坐标集合
    private List<Integer> turnToMysteryIndexes;
    //本 cascade 触发"全部神秘符号同时转换成一个随机符号"时，目标随机符号 id；未触发为 0
    private int mysteryConvertedToIcon;
    //本 cascade 应用的乘倍值（仅在 mystery 触发的兑奖 cascade 上才 > 1）
    private int multiplier;

    public Map<Integer, Integer> getAddIconMap() {
        return addIconMap;
    }

    public void setAddIconMap(Map<Integer, Integer> addIconMap) {
        this.addIconMap = addIconMap;
    }

    public List<SuperGolfAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<SuperGolfAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }

    public List<Integer> getTurnToMysteryIndexes() {
        return turnToMysteryIndexes;
    }

    public void setTurnToMysteryIndexes(List<Integer> turnToMysteryIndexes) {
        this.turnToMysteryIndexes = turnToMysteryIndexes;
    }

    public int getMysteryConvertedToIcon() {
        return mysteryConvertedToIcon;
    }

    public void setMysteryConvertedToIcon(int mysteryConvertedToIcon) {
        this.mysteryConvertedToIcon = mysteryConvertedToIcon;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }
}
