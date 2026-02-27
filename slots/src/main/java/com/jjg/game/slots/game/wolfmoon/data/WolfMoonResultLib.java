package com.jjg.game.slots.game.wolfmoon.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

public class WolfMoonResultLib extends SlotsResultLib<WolfMoonAwardLineInfo> {
    private List<WolfMoonAddIconInfo> addIconInfos;
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
