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
}
