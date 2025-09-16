package com.jjg.game.slots.game.cleopatra.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2025/9/11 11:46
 */
public class CleopatraAddColumnInfo extends AwardLineInfo {
    private int[] arr;
    //中奖图标的坐标
    private Map<Integer, Set<Integer>> winIconIndexMap;

    public int[] getArr() {
        return arr;
    }

    public void setArr(int[] arr) {
        this.arr = arr;
    }

    public Map<Integer, Set<Integer>> getWinIconIndexMap() {
        return winIconIndexMap;
    }

    public void setWinIconIndexMap(Map<Integer, Set<Integer>> winIconIndexMap) {
        this.winIconIndexMap = winIconIndexMap;
    }
}
