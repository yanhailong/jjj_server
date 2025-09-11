package com.jjg.game.slots.game.cleopatra.data;

import com.jjg.game.slots.data.AwardLineInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/11 11:46
 */
public class CleopatraAddColumnInfo extends AwardLineInfo {
    private int[] arr;
    //中奖图标的坐标
    private List<Integer> indexList;

    public int[] getArr() {
        return arr;
    }

    public void setArr(int[] arr) {
        this.arr = arr;
    }

    public List<Integer> getIndexList() {
        return indexList;
    }

    public void setIndexList(List<Integer> indexList) {
        this.indexList = indexList;
    }
}
