package com.jjg.game.slots.game.cleopatra.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/9/11 11:46
 */
public class CleopatraAddColumnInfo extends AwardLineInfo {
    private int[] arr;
    //中奖图标的坐标
    private Map<Integer,List<Integer>> winIconIndexMap;

    public int[] getArr() {
        return arr;
    }

    public void setArr(int[] arr) {
        this.arr = arr;
    }

    public Map<Integer, List<Integer>> getWinIconIndexMap() {
        return winIconIndexMap;
    }

    public void setWinIconIndexMap(Map<Integer, List<Integer>> winIconIndexMap) {
        this.winIconIndexMap = winIconIndexMap;
    }
}
