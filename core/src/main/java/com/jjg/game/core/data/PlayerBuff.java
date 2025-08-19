package com.jjg.game.core.data;

import java.util.List;
import java.util.Map;

/**
 * 玩家buff加成
 * @author 11
 * @date 2025/8/19 11:14
 */
public class PlayerBuff {
    private long playerId;
    //经验或流水加成
    private Map<Integer,List<PlayerBuffDetail>> details;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Map<Integer, List<PlayerBuffDetail>> getDetails() {
        return details;
    }

    public void setDetails(Map<Integer, List<PlayerBuffDetail>> details) {
        this.details = details;
    }
}
