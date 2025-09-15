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

    //奖池金额
    private long mini;
    private long minor;
    private long major;
    private long grand;

    public SuperStarGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }

    public long getMini() {
        return mini;
    }

    public void setMini(long mini) {
        this.mini = mini;
    }

    public long getMinor() {
        return minor;
    }

    public void setMinor(long minor) {
        this.minor = minor;
    }

    public long getMajor() {
        return major;
    }

    public void setMajor(long major) {
        this.major = major;
    }

    public long getGrand() {
        return grand;
    }

    public void setGrand(long grand) {
        this.grand = grand;
    }

    public void setSpinInfo(SuperStarSpinInfo spinInfo) {
        this.spinInfo = spinInfo;
    }

    public SuperStarSpinInfo getSpinInfo() {
        return spinInfo;
    }
}
