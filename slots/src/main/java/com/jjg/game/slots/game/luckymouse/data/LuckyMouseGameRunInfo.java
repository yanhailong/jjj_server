package com.jjg.game.slots.game.luckymouse.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.luckymouse.pb.LuckyMouseWinIconInfo;

import java.util.List;

public class LuckyMouseGameRunInfo extends GameRunInfo<LuckyMousePlayerGameData> {

    private List<LuckyMouseWinIconInfo> awardLineInfos;

    public LuckyMouseGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<LuckyMouseWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<LuckyMouseWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }
}
