package com.jjg.game.slots.game.wolfmoon.data;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
public class WolfMoonAddIconInfo {
    //新增图标: 坐标 -> 图标id
    private Map<Integer, Integer> addIconMap;
    //本次级联步骤的中奖信息
    private List<WolfMoonAwardLineInfo> awardLineInfoList;

    public Map<Integer, Integer> getAddIconMap() {
        return addIconMap;
    }

    public void setAddIconMap(Map<Integer, Integer> addIconMap) {
        this.addIconMap = addIconMap;
    }

    public List<WolfMoonAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<WolfMoonAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }
}
