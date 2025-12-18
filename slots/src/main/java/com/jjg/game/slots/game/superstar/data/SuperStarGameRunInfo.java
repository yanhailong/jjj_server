package com.jjg.game.slots.game.superstar.data;

import com.jjg.game.slots.data.GameRunInfo;
import com.jjg.game.slots.game.superstar.pb.SuperStarSpinInfo;

/**
 * 超级明星
 */
public class SuperStarGameRunInfo extends GameRunInfo<SuperStarPlayerGameData> {

    /**
     * 旋转结果
     */
    private SuperStarSpinInfo spinInfo;


    public SuperStarGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public void setSpinInfo(SuperStarSpinInfo spinInfo) {
        this.spinInfo = spinInfo;
    }

    public SuperStarSpinInfo getSpinInfo() {
        return spinInfo;
    }
}
