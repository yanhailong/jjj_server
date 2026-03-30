package com.jjg.game.slots.data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/7/5 14:35
 */
public class PropAndAwardInfo<T> {
    //cfg.sid -> PropInfo
    private Map<Integer, PropInfo> propMap;
    //配置中的具体id
    private Map<Integer, T> awardMap;
    //配置中的具体id
    private Map<Integer,Map<Integer, T>> awardMap2;


    public PropAndAwardInfo() {
        this.propMap = new HashMap<>();
        this.awardMap = new HashMap<>();
        this.awardMap2 = new HashMap<>();
    }

    public Map<Integer, PropInfo> getPropMap() {
        return propMap;
    }

    public void setPropMap(Map<Integer, PropInfo> propMap) {
        this.propMap = propMap;
    }

    public Map<Integer, T> getAwardMap() {
        return awardMap;
    }

    public void setAwardMap(Map<Integer, T> awardMap) {
        this.awardMap = awardMap;
    }

    public void addProp(int key, PropInfo propInfo) {
        this.propMap.put(key, propInfo);
    }

    public void addAwardInfo(int key, T awardInfo) {
        this.awardMap.put(key, awardInfo);
    }

    public PropInfo getPropInfo(int key) {
        return this.propMap.get(key);
    }

    public T getAwardInfo(int key) {
        return this.awardMap.get(key);
    }

    public void addAwardInfo2(int key1,int key2, T awardInfo){
        this.awardMap2.computeIfAbsent(key1,k->new HashMap<>()).put(key2,awardInfo);
    }

    public Map<Integer, Map<Integer, T>> getAwardMap2() {
        return awardMap2;
    }
}
