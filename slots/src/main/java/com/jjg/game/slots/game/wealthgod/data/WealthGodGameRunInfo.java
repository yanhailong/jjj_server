package com.jjg.game.slots.game.wealthgod.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodSpinInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 财神游戏
 */
public class WealthGodGameRunInfo extends GameRunInfo<WealthGodPlayerGameData> {

    /**
     * 所有的免费旋转的结果
     */
    private List<WealthGodSpinInfo> spinInfo = new ArrayList<>();

    /**
     * 获得的奖池金额
     */
    public long jackpotValue;
    /**
     * 玩家获取的奖池id
     */
    private int jackpotId;

    /**
     * jackpot奖励给玩家后剩余的奖池金额
     */
    private long poolValue;

    public WealthGodGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public List<WealthGodSpinInfo> getSpinInfo() {
        return spinInfo;
    }

    public void setSpinInfo(List<WealthGodSpinInfo> spinInfo) {
        this.spinInfo = spinInfo;
    }

    public long getJackpotValue() {
        return jackpotValue;
    }

    public void setJackpotValue(long jackpotValue) {
        this.jackpotValue = jackpotValue;
    }

    public long getPoolValue() {
        return poolValue;
    }

    public void setPoolValue(long poolValue) {
        this.poolValue = poolValue;
    }
}
