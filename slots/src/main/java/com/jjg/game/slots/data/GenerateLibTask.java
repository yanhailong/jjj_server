package com.jjg.game.slots.data;

import java.util.Map;

public class GenerateLibTask {
    private int gameType;
    private Map<Integer, Integer> libTypeCountMap;

    public GenerateLibTask(int gameType, Map<Integer, Integer> libTypeCountMap) {
        this.gameType = gameType;
        this.libTypeCountMap = libTypeCountMap;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public Map<Integer, Integer> getLibTypeCountMap() {
        return libTypeCountMap;
    }

    public void setLibTypeCountMap(Map<Integer, Integer> libTypeCountMap) {
        this.libTypeCountMap = libTypeCountMap;
    }
}
