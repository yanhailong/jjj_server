package com.jjg.game.slots.game.wealthgod.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodSpinInfo;

/**
 * 财神游戏
 */
public class WealthGodGameRunInfo extends GameRunInfo<WealthGodPlayerGameData> {

    /**
     * 旋转的结果
     */
    private WealthGodSpinInfo spinInfo;

    /**
     * 玩家获取的奖池id
     */
    private int jackpotId;

    public WealthGodGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public WealthGodSpinInfo getSpinInfo() {
        return spinInfo;
    }

    public void setSpinInfo(WealthGodSpinInfo spinInfo) {
        this.spinInfo = spinInfo;
    }

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }
}
