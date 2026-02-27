package com.jjg.game.slots.game.hulk.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.hulk.pb.HulkWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/1/15
 */
public class HulkGameRunInfo extends GameRunInfo<HulkPlayerGameData> {
    //中奖线信息
    private List<HulkWinIconInfo> awardLineInfos;

    public HulkGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public List<HulkWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<HulkWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }
}
