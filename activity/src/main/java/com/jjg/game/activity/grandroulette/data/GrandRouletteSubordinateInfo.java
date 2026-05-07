package com.jjg.game.activity.grandroulette.data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lm
 * @date 2026/4/22 16:26
 */
public class GrandRouletteSubordinateInfo {
    /**
     * 下级信息
     */
    Map<Long, Integer> subordinateMap = new HashMap<>();

    public Map<Long, Integer> getSubordinateMap() {
        return subordinateMap;
    }

    public void setSubordinateMap(Map<Long, Integer> subordinateMap) {
        this.subordinateMap = subordinateMap;
    }
}
