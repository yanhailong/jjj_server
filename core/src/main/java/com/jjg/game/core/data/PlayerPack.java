package com.jjg.game.core.data;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 玩家背包数据
 * @author 11
 * @date 2025/8/6 15:41
 */
@Document
public class PlayerPack {
    //玩家id
    private long playerId;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }
}
