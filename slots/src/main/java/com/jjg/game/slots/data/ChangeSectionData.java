package com.jjg.game.slots.data;

import java.util.Map;

/**
 * @author 11
 * @date 2026/3/3
 */
public class ChangeSectionData {
    //下注下限
    private long betMin;
    //下注上限
    private long betMax;
    //修改后的概率
    private Map<Integer,PropInfo> sectionPropMap;

    public long getBetMin() {
        return betMin;
    }

    public void setBetMin(long betMin) {
        this.betMin = betMin;
    }

    public long getBetMax() {
        return betMax;
    }

    public void setBetMax(long betMax) {
        this.betMax = betMax;
    }

    public Map<Integer, PropInfo> getSectionPropMap() {
        return sectionPropMap;
    }

    public void setSectionPropMap(Map<Integer, PropInfo> sectionPropMap) {
        this.sectionPropMap = sectionPropMap;
    }
}
