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
    //自动兑现配置 (betIndex -> 目标倍率, 万分比); 跨回合保留，由玩家通过 reqAutoCashOut 维护
    private Map<Integer,Integer> autoCashOutTargetMap = new HashMap<>();

    public Map<Integer, AirRaidBetData> getAirRaidBetDataMap() {
        return airRaidBetDataMap;
    }

    public void setAirRaidBetDataMap(Map<Integer, AirRaidBetData> airRaidBetDataMap) {
        this.airRaidBetDataMap = airRaidBetDataMap;
    }

    public Map<Integer, Integer> getAutoCashOutTargetMap() {
        return autoCashOutTargetMap;
    }

    public void setAutoCashOutTargetMap(Map<Integer, Integer> autoCashOutTargetMap) {
        this.autoCashOutTargetMap = autoCashOutTargetMap;
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
