package com.jjg.game.slots.game.demonchild.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.demonchild.pb.bean.DemonChildLineInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:55
 */
public class DemonChildGameRunInfo extends GameRunInfo<DemonChildPlayerGameData> {
    //总免费次数
    private int totalFreeCount;

    public int getTotalFreeCount() {
        return totalFreeCount;
    }

    public void setTotalFreeCount(int totalFreeCount) {
        this.totalFreeCount = totalFreeCount;
    }

    public DemonChildGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    //中奖线信息
    private List<DemonChildLineInfo> awardLineInfos;

    public List<DemonChildLineInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<DemonChildLineInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }
}
