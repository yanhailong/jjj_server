package com.jjg.game.slots.game.frozenThrone.data;

import com.jjg.game.slots.data.GameRunInfo;

import java.util.Set;

/**
 * @author lihaocao
 * @date 2025/12/2 17:55
 */
public class FrozenThroneGameRunInfo extends GameRunInfo<FrozenThronePlayerGameData> {
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

    public FrozenThroneGameRunInfo(int code, long playerId) {
        super(code, playerId);
    }
}
