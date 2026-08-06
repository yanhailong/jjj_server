package com.jjg.game.sim.data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2026/6/11
 */
public class SlotsSpinResult {
    private Map<Integer, Long> itemsMap;
    private int power;
    private int researchPoints;
    //今日剩余试玩次数
    private int remainingTrials;

    public Map<Integer, Long> getItemsMap() {
        return itemsMap;
    }

    public void setItemsMap(Map<Integer, Long> itemsMap) {
        this.itemsMap = itemsMap;
    }

    /**
     * 并入本次旋转另行结算的掉落 (赛季宝石), 随 rpc 结果一起下发给客户端。
     * 已有 itemsMap 可能是不可变表, 故合并到新表再回写。
     */
    public void mergeItems(Map<Integer, Long> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Integer, Long> merged = new HashMap<>(itemsMap == null ? Map.of() : itemsMap);
        items.forEach((itemId, count) -> merged.merge(itemId, count, Long::sum));
        itemsMap = merged;
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

    public int getRemainingTrials() {
        return remainingTrials;
    }

    public void setRemainingTrials(int remainingTrials) {
        this.remainingTrials = remainingTrials;
    }
}
