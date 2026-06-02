package com.jjg.game.slots.game.bountyduel.data;

import java.util.List;
import java.util.Map;

/**
 * 一次消除下落后的补图信息。
 */
public class BountyDuelAddIconInfo {
    /**
     * 新增图标信息：位置 -> 图标 ID。
     */
    private Map<Integer, Integer> addIconMap;
    /**
     * 补图后再次中奖的信息。
     */
    private List<BountyDuelAwardLineInfo> awardLineInfoList;

    public Map<Integer, Integer> getAddIconMap() {
        return addIconMap;
    }

    public void setAddIconMap(Map<Integer, Integer> addIconMap) {
        this.addIconMap = addIconMap;
    }

    public List<BountyDuelAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<BountyDuelAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }
}
