package com.jjg.game.slots.game.bountyduel.data;

import com.jjg.game.slots.data.SlotsResultLib;

import java.util.List;

/**
 * 赏金大对决结果库数据。
 */
public class BountyDuelResultLib extends SlotsResultLib<BountyDuelAwardLineInfo> {
    /**
     * 消除后补图和再次中奖信息。
     */
    private List<BountyDuelAddIconInfo> addIconInfos;
    /**
     * 本局触发或追加的免费次数。
     */
    private int addFreeCount;

    public List<BountyDuelAddIconInfo> getAddIconInfos() {
        return addIconInfos;
    }

    public void setAddIconInfos(List<BountyDuelAddIconInfo> addIconInfos) {
        this.addIconInfos = addIconInfos;
    }

    public int getAddFreeCount() {
        return addFreeCount;
    }

    public void setAddFreeCount(int addFreeCount) {
        this.addFreeCount = addFreeCount;
    }
}
