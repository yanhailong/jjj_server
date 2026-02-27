package com.jjg.game.slots.game.wolfmoon.data;

import java.util.List;
import java.util.Map;

public class WolfMoonAddIconInfo {
    private Map<Integer, Integer> addIconMap;
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
