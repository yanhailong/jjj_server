package com.jjg.game.dollarexpress.data;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/23 10:51
 */
public class LineInfo {
    //中奖线id
    private int lineId;
    //中奖图标
    private List<Integer> arardIconList;

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public List<Integer> getArardIconList() {
        return arardIconList;
    }

    public void setArardIconList(List<Integer> arardIconList) {
        this.arardIconList = arardIconList;
    }
}
