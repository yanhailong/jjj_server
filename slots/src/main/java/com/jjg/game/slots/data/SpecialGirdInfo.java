package com.jjg.game.slots.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 特殊格子信息
 * @author 11
 * @date 2025/8/30 9:23
 */
public class SpecialGirdInfo {
    private int cfgId;
    //值
    private Map<Integer,Integer> valueMap;
    //值类型
    private int valueType;
    //被动游戏id
    private int miniGameId;

    public int getCfgId() {
        return cfgId;
    }

    public void setCfgId(int cfgId) {
        this.cfgId = cfgId;
    }

    public Map<Integer, Integer> getValueMap() {
        return valueMap;
    }

    public void setValueMap(Map<Integer, Integer> valueMap) {
        this.valueMap = valueMap;
    }

    public int getValueType() {
        return valueType;
    }

    public void setValueType(int valueType) {
        this.valueType = valueType;
    }

    public int getMiniGameId() {
        return miniGameId;
    }

    public void setMiniGameId(int miniGameId) {
        this.miniGameId = miniGameId;
    }

    public void addValue(int k,int v) {
        if(this.valueMap == null){
            this.valueMap = new HashMap<>();
        }
        this.valueMap.put(k,v);
    }
}
