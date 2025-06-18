package com.jjg.game.dollarexpress.data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/16 13:40
 */
public class PropInfo<T> {
    private int sum;
    private Map<T, PropData<T>> propMap = new HashMap<>();

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }

    public Map<T, PropData<T>> getPropMap() {
        return propMap;
    }
}
