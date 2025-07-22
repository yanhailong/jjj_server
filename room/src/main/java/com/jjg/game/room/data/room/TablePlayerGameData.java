package com.jjg.game.room.data.room;

import com.jjg.game.common.proto.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * table类的玩家数据保存
 *
 * @author 2CL
 */
public class TablePlayerGameData {
    // 开局时玩家的座位号，在场上显示的人才有座位号
    private int sitNum;
    // 最近20场的下注数据 / 临时数据
    private List<Pair<Boolean, Long>> betInfoList = new ArrayList<>();
    // 红黑大战玩家本场此押注总金额
    private long totalBet;

    public void addBetRecord(long getGold) {
        int i = (betInfoList.size() - 20) + 1;
        if (i > 0) {
            betInfoList.subList(0, i).clear();
        }
        betInfoList.add(Pair.newPair(getGold > 0, totalBet));
        totalBet = 0;
    }

    public List<Pair<Boolean, Long>> getBetInfoList() {
        if (betInfoList == null) {
            betInfoList = new ArrayList<>();
        }
        return betInfoList;
    }

    public void addTotalBet(long bet){
        this.totalBet += bet;
    }
    public int getSitNum() {
        return sitNum;
    }

    public void setSitNum(int sitNum) {
        this.sitNum = sitNum;
    }
}
