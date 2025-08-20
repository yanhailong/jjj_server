package com.jjg.game.hall.casino.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/8/16 16:06
 */
@Document
public class PlayerBuilding {
    //玩家id
    @Id
    private Long playerId;
    //楼层信息 赌场id->赌场信息
    private Map<Integer, CasinoInfo> buildingData;

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Map<Integer, CasinoInfo> getBuildingData() {
        return buildingData;
    }

    public void setBuildingData(Map<Integer, CasinoInfo> buildingData) {
        this.buildingData = buildingData;
    }
}
