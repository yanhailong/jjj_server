package com.jjg.game.slots.game.christmasBashNight.data;

import com.jjg.game.slots.data.GameRunInfo;

/**
 * @author lihaocao
 * @date 2025/12/2 17:55
 */
public class ChristmasBashNightGameRunInfo extends GameRunInfo<ChristmasBashNightPlayerGameData> {
    public long mini;
    public long minor;
    public long major;
    public long grand;

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

    public ChristmasBashNightGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }
}
