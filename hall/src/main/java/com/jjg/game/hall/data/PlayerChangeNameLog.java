package com.jjg.game.hall.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2025/8/7 16:13
 */
@Document
public class PlayerChangeNameLog {
    @Id
    private long playerId;
    //最近一次改名时间
    private int lastChangeTime;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getLastChangeTime() {
        return lastChangeTime;
    }

    public void setLastChangeTime(int lastChangeTime) {
        this.lastChangeTime = lastChangeTime;
    }
}
