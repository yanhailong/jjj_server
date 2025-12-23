package com.jjg.game.slots.game.thor.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.thor.pb.ThorWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/12/1 18:10
 */
public class ThorGameRunInfo extends GameRunInfo<ThorPlayerGameData> {
    //中奖线信息
    private List<ThorWinIconInfo> awardLineInfos;
    //免费游戏累计金额
    private long freeModeTotalReward;
    //标记免费模式结束
    private boolean freeEnd;

    public ThorGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<ThorWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<ThorWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }

    public long getFreeModeTotalReward() {
        return freeModeTotalReward;
    }

    public void setFreeModeTotalReward(long freeModeTotalReward) {
        this.freeModeTotalReward = freeModeTotalReward;
    }

    public boolean isFreeEnd() {
        return freeEnd;
    }

    public void setFreeEnd(boolean freeEnd) {
        this.freeEnd = freeEnd;
    }
}
