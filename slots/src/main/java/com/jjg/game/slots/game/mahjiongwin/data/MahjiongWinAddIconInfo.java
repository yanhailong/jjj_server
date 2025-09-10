package com.jjg.game.slots.game.mahjiongwin.data;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/9/8 16:26
 */
public class MahjiongWinAddIconInfo {
    //添加的图案， 坐标 -> 图标id
    private Map<Integer, Integer> addIconMap;
    //奖励
    private List<MahjiongWinAwardLineInfo> awardLineInfoList;

    public Map<Integer, Integer> getAddIconMap() {
        return addIconMap;
    }

    public void setAddIconMap(Map<Integer, Integer> addIconMap) {
        this.addIconMap = addIconMap;
    }

    public List<MahjiongWinAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<MahjiongWinAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }
}
