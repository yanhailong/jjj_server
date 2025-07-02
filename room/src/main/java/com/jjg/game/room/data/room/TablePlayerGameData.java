package com.jjg.game.room.data.room;

import java.util.LinkedList;

/**
 * table类的玩家数据保存
 *
 * @author 2CL
 */
public class TablePlayerGameData {
    // 玩家百家乐最近20场的下注数据 / 临时数据
    private final LinkedList<Long> betCostStatusRecord = new LinkedList<>();
    // 开局时玩家的座位号，在场上显示的人才有座位号
    private int sitNum;

    public LinkedList<Long> getBetCostStatusRecord() {
        return betCostStatusRecord;
    }

    public void addRecord(long goldCost) {
        if (betCostStatusRecord.size() >= 20) {
            betCostStatusRecord.removeFirst();
        }
        betCostStatusRecord.addLast(goldCost);
    }

    public int getSitNum() {
        return sitNum;
    }

    public void setSitNum(int sitNum) {
        this.sitNum = sitNum;
    }
}
