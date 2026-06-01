package com.jjg.game.slots.game.hotfootball.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:30
 */
public class HotFootballResultLib extends SlotsResultLib<HotFootballAwardLineInfo> {
    //消除补齐的信息
    private List<HotFootballAddIconInfo> addIconInfos;
    //增加的免费次数
    private int addFreeCount;
    //本局免费 spin 应用的乘倍值（能量值/守门员机制，文档 [37-43]）
    private int multiplier;
    //本局结束后的能量值
    private int energyAfter;
    //本局结束后的能量满值上限（6→8→10→...→16）
    private int maxEnergyAfter;

    public List<HotFootballAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<HotFootballAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }

    public int getEnergyAfter() {
        return energyAfter;
    }

    public void setEnergyAfter(int energyAfter) {
        this.energyAfter = energyAfter;
    }

    public int getMaxEnergyAfter() {
        return maxEnergyAfter;
    }

    public void setMaxEnergyAfter(int maxEnergyAfter) {
        this.maxEnergyAfter = maxEnergyAfter;
    }
}
