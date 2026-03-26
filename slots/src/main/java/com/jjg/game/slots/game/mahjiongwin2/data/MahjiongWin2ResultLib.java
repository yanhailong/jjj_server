package com.jjg.game.slots.game.mahjiongwin2.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:30
 */
public class MahjiongWin2ResultLib extends SlotsResultLib<MahjiongWin2AwardLineInfo> {
    //消除补齐的信息
    private List<MahjiongWin2AddIconInfo> addIconInfos;
    //增加的免费次数
    private int addFreeCount;

    public List<MahjiongWin2AddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<MahjiongWin2AddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
