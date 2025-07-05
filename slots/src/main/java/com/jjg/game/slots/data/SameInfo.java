package com.jjg.game.slots.data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/18 18:29
 */
public class SameInfo {
    private int baseIconId;
    private boolean same;
    private Map<Integer,Integer> specialIconShowMap;

    public int getBaseIconId() {
        return baseIconId;
    }

    public void setBaseIconId(int baseIconId) {
        this.baseIconId = baseIconId;
    }

    public boolean isSame() {
        return same;
    }

    public void setSame(boolean same) {
        this.same = same;
    }

    public Map<Integer, Integer> getSpecialIconShowMap() {
        return specialIconShowMap;
    }

    public void addSpecialIconShow(int iconId) {
        if(this.specialIconShowMap == null) {
            this.specialIconShowMap = new HashMap<>();
        }
        this.specialIconShowMap.merge(iconId, 1, Integer::sum);
    }
}
