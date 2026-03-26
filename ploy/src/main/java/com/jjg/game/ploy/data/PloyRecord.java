package com.jjg.game.ploy.data;

import org.springframework.data.mongodb.core.index.CompoundIndex;

/**
 * @author 11
 * @date 2026/3/24
 */
@CompoundIndex(
        name = "ploy_record_query_idx",
        def = "{'playerId': 1, 'roomCfgId': 1, 'timestamp': -1}"
)
public abstract class PloyRecord {
    //玩家id
    private long playerId;
    //场次id
    private int roomCfgId;
    //时间
    private long timestamp;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
