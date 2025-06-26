package com.jjg.game.dollarexpress.data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/16 13:40
 */
public class PropInfo {
    private int sum;
    private Map<Integer, int[]> propMap = new HashMap<>();

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    public Map<Integer, int[]> getPropMap() {
        return propMap;
    }

    public void setPropMap(Map<Integer, int[]> propMap) {
        this.propMap = propMap;
    }

    public void addSum(int value){
        this.sum += value;
    }
}
