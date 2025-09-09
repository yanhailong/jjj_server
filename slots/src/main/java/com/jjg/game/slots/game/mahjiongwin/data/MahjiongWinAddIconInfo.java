package com.jjg.game.slots.game.mahjiongwin.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/9/8 16:26
 */
public class MahjiongWinAddIconInfo {
    //添加的图案，列 -> 图标坐标
    private Map<Integer, Integer> addIconCountMap;
    //奖励
    private List<MahjiongWinAwardLineInfo> awardLineInfoList;

    public Map<Integer, Integer> getAddIconCountMap() {
        return addIconCountMap;
    }

    public void setAddIconCountMap(Map<Integer, Integer> addIconCountMap) {
        this.addIconCountMap = addIconCountMap;
    }

    public List<MahjiongWinAwardLineInfo> getAwardLineInfoList() {
        return awardLineInfoList;
    }

    public void setAwardLineInfoList(List<MahjiongWinAwardLineInfo> awardLineInfoList) {
        this.awardLineInfoList = awardLineInfoList;
    }

    public void addIconCount(int colIndex, int count) {
        if(this.addIconCountMap == null) {
            this.addIconCountMap = new HashMap<>();
        }
        this.addIconCountMap.put(colIndex, count);
    }
}
