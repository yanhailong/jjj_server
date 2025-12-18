package com.jjg.game.slots.game.pegasusunbridle.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.pegasusunbridle.pb.bean.PegasusUnbridleWinIconInfo;
import com.jjg.game.slots.game.thor.pb.ThorWinIconInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/1 17:55
 */
public class PegasusUnbridleGameRunInfo extends GameRunInfo<PegasusUnbridlePlayerGameData> {

    public PegasusUnbridleGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    //中奖线信息
    private List<PegasusUnbridleWinIconInfo> awardLineInfos;

    public List<PegasusUnbridleWinIconInfo> getAwardLineInfos() {
        return awardLineInfos;
    }

    public void setAwardLineInfos(List<PegasusUnbridleWinIconInfo> awardLineInfos) {
        this.awardLineInfos = awardLineInfos;
    }
}
