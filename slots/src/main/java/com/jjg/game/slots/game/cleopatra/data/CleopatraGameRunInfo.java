package com.jjg.game.slots.game.cleopatra.data;

import com.jjg.game.slots.data.GameRunInfo;

/**
 * @author 11
 * @date 2025/8/27 10:16
 */
public class CleopatraGameRunInfo extends GameRunInfo<CleopatraPlayerGameData> {
    private long currentPoolValue;

    public CleopatraGameRunInfo(int code,long playerId) {
        super(code,playerId);
    }

    public long getCurrentPoolValue() {
        return currentPoolValue;
    }

    public void setCurrentPoolValue(long currentPoolValue) {
        this.currentPoolValue = currentPoolValue;
    }
}
