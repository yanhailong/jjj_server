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
    private final Map<Integer, Map<Long, Long>> bankerChangeMap = new HashMap<>();
    /**
     * 是否初始化
     */
    boolean isInit = false;
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

    public Map<Integer, Map<Long, Long>> getBankerChangeMap() {
        return bankerChangeMap;
    }

    public boolean isInit() {
        return isInit;
    }

    public void addBankerChangeGold(long value) {
        this.bankerChangeGold += value;
    }

    public long getBankerChangeGold() {
        return bankerChangeGold;
    }

    /**
     * 初始化数据
     *
     * @param betInfo 下注信息
     */
    public void initData(Map<Integer, Map<Long, Long>> betInfo) {
        bankerChangeMap.putAll(betInfo);
        isInit = true;
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
