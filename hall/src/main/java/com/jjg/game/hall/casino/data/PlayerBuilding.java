package com.jjg.game.hall.casino.data;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author lm
 * @date 2025/8/16 16:06
 */
@Document
public class PlayerBuilding {
    //玩家id
    private long playerId;
    //赌场id
    private int casinoId;
    //楼层信息 赌场id->赌场信息
    private CasinoInfo casinoInfo;

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }
    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getCasinoId() {
        return casinoId;
    }

    public void setCasinoId(int casinoId) {
        this.casinoId = casinoId;
    }

    public CasinoInfo getCasinoInfo() {
        return casinoInfo;
    }

    public void setCasinoInfo(CasinoInfo casinoInfo) {
        this.casinoInfo = casinoInfo;
    }

    @Override
    public String toString() {
        return "PlayerBuilding{" +
                "playerId=" + playerId +
                ", casinoId=" + casinoId +
                ", casinoInfo=" + casinoInfo +
                '}';
    }
}
