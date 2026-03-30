package com.jjg.game.slots.game.basketballSuperstar.data;

import java.util.Map;

/**
 * @author lihaocao
 * @date 2025/9/10 9:42
 */
public class BasketballSuperstarFreeStickyWildInfo {
    //什么类型触发
    private int libType;
    //图标权重map
    private Map<Integer,Integer> iconWeightMap;


    public int getLibType() {
        return libType;
    }

    public void setLibType(int libType) {
        this.libType = libType;
    }

    public Map<Integer, Integer> getIconWeightMap() {
        return iconWeightMap;
    }

    public void setIconWeightMap(Map<Integer, Integer> iconWeightMap) {
        this.iconWeightMap = iconWeightMap;
    }
}
