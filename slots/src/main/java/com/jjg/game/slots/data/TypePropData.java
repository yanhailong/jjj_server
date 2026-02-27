package com.jjg.game.slots.data;


import java.util.Map;

/**
 * @author 11
 * @date 2026/2/25
 */
public class TypePropData {
    private long begin;
    private long end;
    private Map<Integer,Integer> propMap;
    //权重信息
    //specialResultLib表中typeProp字段的随机权重信息
    private PropInfo typePropInfo;
    //specialResultLib表中typeProp字段(排除了jackpot类型)的随机权重信息
    private PropInfo noJackpotTypePropInfo;

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public Map<Integer, Integer> getPropMap() {
        return propMap;
    }

    public void setPropMap(Map<Integer, Integer> propMap) {
        this.propMap = propMap;
    }

    public PropInfo getTypePropInfo() {
        return typePropInfo;
    }

    public void setTypePropInfo(PropInfo typePropInfo) {
        this.typePropInfo = typePropInfo;
    }

    public PropInfo getNoJackpotTypePropInfo() {
        return noJackpotTypePropInfo;
    }

    public void setNoJackpotTypePropInfo(PropInfo noJackpotTypePropInfo) {
        this.noJackpotTypePropInfo = noJackpotTypePropInfo;
    }
}
