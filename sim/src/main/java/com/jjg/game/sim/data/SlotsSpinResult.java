package com.jjg.game.sim.data;

import java.util.Map;

/**
 * @author 11
 * @date 2026/6/11
 */
public class SlotsSpinResult {
    private Map<Integer, Long> itemsMap;
    private int power;
    private int researchPoints;

    public Map<Integer, Long> getItemsMap() {
        return itemsMap;
    }

    public void setItemsMap(Map<Integer, Long> itemsMap) {
        this.itemsMap = itemsMap;
    }

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getResearchPoints() {
        return researchPoints;
    }

    public void setResearchPoints(int researchPoints) {
        this.researchPoints = researchPoints;
    }
}
