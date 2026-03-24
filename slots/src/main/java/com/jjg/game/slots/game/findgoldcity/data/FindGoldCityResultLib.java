package com.jjg.game.slots.game.findgoldcity.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/2 17:30
 */
public class FindGoldCityResultLib extends SlotsResultLib<FindGoldCityAwardLineInfo> {
    //消除补齐的信息
    private List<FindGoldCityAddIconInfo> addIconInfos;
    //增加的免费次数
    private int addFreeCount;
    //当前倍数
    private int currentMultiple;

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }

    public List<FindGoldCityAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<FindGoldCityAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getCurrentMultiple() {
        return currentMultiple;
    }

    public void setCurrentMultiple(int currentMultiple) {
        this.currentMultiple = currentMultiple;
    }
}

