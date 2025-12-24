package com.jjg.game.slots.game.goldsnakefortune.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.goldsnakefortune.pb.GoldSnakeFortuneWinIconInfo;

import java.util.List;

public class GoldSnakeFortuneGameRunInfo extends GameRunInfo<GoldSnakeFortunePlayerGameData> {
    //中奖线信息
    private List<GoldSnakeFortuneWinIconInfo> awardLineInfos;

    public GoldSnakeFortuneGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<GoldSnakeFortuneWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<GoldSnakeFortuneWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }
}
