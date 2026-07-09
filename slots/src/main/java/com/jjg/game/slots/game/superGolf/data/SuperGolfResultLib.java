package com.jjg.game.slots.game.superGolf.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

public class SuperGolfResultLib extends SlotsResultLib<SuperGolfAwardLineInfo> {
    //cascade 链
    private List<SuperGolfAddIconInfo> addIconInfos;
    //本局触发再次免费的局数（盘面 4+ scatter 时按表追加）
    private int addFreeCount;
    //本局应用的最终乘倍值：
    //  普通模式：盘面神秘符号数（兑奖后置 1，无中奖时为 1）
    //  免费模式：本局贡献的"新增"乘倍数；累计值由 PlayerGameData.freeMultiplierAccum 持有
    private int multiplier = 1;

    public List<SuperGolfAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<SuperGolfAddIconInfo> addIconInfos) {
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
}
