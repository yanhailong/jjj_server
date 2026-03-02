package com.jjg.game.slots.game.wolfmoon.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
public class WolfMoonResultLib extends SlotsResultLib<WolfMoonAwardLineInfo> {
    //消除补齐的信息
    private List<WolfMoonAddIconInfo> addIconInfos;
    //增加的免费次数
    private int addFreeCount;

    public List<WolfMoonAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<WolfMoonAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
