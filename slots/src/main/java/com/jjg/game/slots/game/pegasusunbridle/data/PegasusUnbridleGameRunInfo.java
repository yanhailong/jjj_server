package com.jjg.game.slots.game.pegasusunbridle.data;

import com.jjg.game.slots.data.GameRunInfo;

/**
 * @author 11
 * @date 2025/8/1 17:55
 */
public class PegasusUnbridleGameRunInfo extends GameRunInfo<PegasusUnbridlePlayerGameData> {

    public PegasusUnbridleGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }
    private long mini;
    private long minor;
    private long major;
    private long grand;


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
}
