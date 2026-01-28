package com.jjg.game.slots.game.zeusVsHades.data;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;

import java.util.List;
import java.util.Set;

/**
 * @author 11
 * @date 2026/1/20 15:39
 */
public class ZeusVsHadesSpecialAuxiliaryInfo extends SpecialAuxiliaryInfo {
    //需要变成wild的图标
    private Set<Integer> hadesExchangeWildSet;
    //需要变成大wild的列
    private Integer column;
    //列wild倍数
    private Integer time;
    //1宙斯赢 2哈迪斯赢
    private Integer wildStatus;

    public Integer getWildStatus() {
        return wildStatus;
    }

    public void setWildStatus(Integer wildStatus) {
        this.wildStatus = wildStatus;
    }

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public Integer getColumn() {
        return column;
    }

    public void setColumn(Integer column) {
        this.column = column;
    }

    public Set<Integer> getHadesExchangeWildSet() {
        return hadesExchangeWildSet;
    }

    public void setHadesExchangeWildSet(Set<Integer> hadesExchangeWildSet) {
        this.hadesExchangeWildSet = hadesExchangeWildSet;
    }
}
