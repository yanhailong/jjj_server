package com.jjg.game.room.data.room;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/11/6 10:38
 */
public class RoomBankerChangeParam {
    /**
     * 获胜的押注列表
     */
    private final Map<Integer, Map<Long, Integer>> bankerChangeMap = new HashMap<>();
    /**
     * 总税收
     */
    private long totalTaxRevenue;
    /**
     * 房主税收总收益
     */
    private long roomCreatorTotalIncome;

    /**
     * 下注总收益
     *
     */
    private long bankerChangeGold;

    public Map<Integer, Map<Long, Integer>> getBankerChangeMap() {
        return bankerChangeMap;
    }


    public void addBankerChangeGold(long value) {
        this.bankerChangeGold += value;
    }

    public long getBankerChangeGold() {
        return bankerChangeGold;
    }

    /**
     * 初始化数据
     * @param betInfo 下注信息
     */
    public void initData(Map<Integer, Map<Long, List<Integer>>> betInfo) {
        for (Map.Entry<Integer, Map<Long, List<Integer>>> entry : betInfo.entrySet()) {
            Map<Long, Integer> longIntegerMap = bankerChangeMap.computeIfAbsent(entry.getKey(), key -> new HashMap<>());
            for (Map.Entry<Long, List<Integer>> listEntry : entry.getValue().entrySet()) {
                longIntegerMap.put(listEntry.getKey(), listEntry.getValue().stream().mapToInt(Integer::intValue).sum());
            }
        }
    }

    public void removeArea(int areaId) {
        bankerChangeMap.remove(areaId);
    }

    public void clearArea() {
        bankerChangeMap.clear();
    }

    public void addTotalTaxRevenue(long add) {
        totalTaxRevenue += add;
    }

    public void addRoomCreatorTotalIncome(long add) {
        roomCreatorTotalIncome += add;
    }

    public long getTotalTaxRevenue() {
        return totalTaxRevenue;
    }


    public long getRoomCreatorTotalIncome() {
        return roomCreatorTotalIncome;
    }


}
