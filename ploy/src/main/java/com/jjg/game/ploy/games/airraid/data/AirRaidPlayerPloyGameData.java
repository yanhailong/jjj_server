package com.jjg.game.ploy.games.airraid.data;

import com.jjg.game.ploy.data.PlayerMultiPloyGameData;

import java.util.HashMap;
import java.util.Map;

/**
 * 空袭游戏 — 玩家数据
 *
 * @author 11
 * @date 2026/3/19
 */
public class AirRaidPlayerPloyGameData extends PlayerMultiPloyGameData {
    //两次下注信息
    private Map<Integer,AirRaidBetData> airRaidBetDataMap = new HashMap<>();

    public Map<Integer, AirRaidBetData> getAirRaidBetDataMap() {
        return airRaidBetDataMap;
    }

    public void setAirRaidBetDataMap(Map<Integer, AirRaidBetData> airRaidBetDataMap) {
        this.airRaidBetDataMap = airRaidBetDataMap;
    }

    /**
     * 设置下注额
     * @param bet
     * @param betIndex
     */
    public void addBetValue(long bet,int betIndex){
        this.airRaidBetDataMap.computeIfAbsent(betIndex, k -> new AirRaidBetData(bet));
    }
}
