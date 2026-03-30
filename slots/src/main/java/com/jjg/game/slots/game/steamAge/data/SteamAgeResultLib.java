package com.jjg.game.slots.game.steamAge.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/12/2 17:30
 */
public class SteamAgeResultLib extends SlotsResultLib<SteamAgeAwardLineInfo> {
    //增加的免费次数
    private int addFreeCount;
    //免费 新增列次数
    private int expandTimes;
    //消除补齐的信息
    private List<SteamAgeExpandIconInfo> addIconInfos;

    public List<SteamAgeExpandIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<SteamAgeExpandIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }

    public int getExpandTimes() {
        return expandTimes;
    }

    public void setExpandTimes(int expandTimes) {
        this.expandTimes = expandTimes;
    }
}
