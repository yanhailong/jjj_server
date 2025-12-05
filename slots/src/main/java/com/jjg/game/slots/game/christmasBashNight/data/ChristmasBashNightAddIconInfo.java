package com.jjg.game.slots.game.christmasBashNight.data;

import java.util.List;
import java.util.Map;

/**
 * @author lihaocao
 * @date 2025/9/8 16:26
 */
public class ChristmasBashNightAddIconInfo {
    //添加的图案， 坐标 -> 图标id
    private Map<Integer, Integer> addIconMap;
    //奖励
    private List<ChristmasBashNightAwardLineInfo> awardLineInfoList;

    public Map<Integer, Integer> getAddIconMap() {
        return addIconMap;
    }

    public void setAddIconMap(Map<Integer, Integer> addIconMap) {
        this.addIconMap = addIconMap;
    }

    public List<ChristmasBashNightAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<ChristmasBashNightAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }
}
