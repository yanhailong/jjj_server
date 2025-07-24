package com.jjg.game.table.common.data;

import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.bean.Room_BetCfg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对战类游戏的内存常驻数据 Value Object
 *
 * @author 2CL
 */
public class TableGameDataVo extends GameDataVo<Room_BetCfg> {

    // 玩家下注数据 玩家ID：下注区域ID：下注金额列表
    protected final Map<Long, Map<Integer, List<Integer>>> playerBetInfo = new HashMap<>();

    public void clearRoundData() {
        // 玩家的区域押注信息清除
        playerBetInfo.clear();
    }

    public Map<Integer, List<Integer>> getPlayerBetInfo(long playerId) {
        return playerBetInfo.getOrDefault(playerId, new HashMap<>());
    }

    public Map<Long, Map<Integer, List<Integer>>> getPlayerBetInfo() {
        return playerBetInfo;
    }

    public void updatePlayerBetInfo(long playerId, Map<Integer, List<Integer>> betInfo) {
        playerBetInfo.put(playerId, betInfo);
    }

    public Long getAreaTotalBet(int areaIdx) {
        long areaTotal = 0;
        for (Map<Integer, List<Integer>> value : playerBetInfo.values()) {
            if (value.containsKey(areaIdx)) {
                areaTotal += value.get(areaIdx).stream().mapToInt(Integer::intValue).sum();
            }
        }
        return areaTotal;
    }

    public Map<Integer, Long> getAreaTotalBetMap() {
        Map<Integer, Long> areaTotal = new HashMap<>();
        for (Map<Integer, List<Integer>> value : playerBetInfo.values()) {
            for (Map.Entry<Integer, List<Integer>> entry : value.entrySet()) {
                long areaIdxTotal = entry.getValue().stream().mapToLong(Integer::longValue).sum();
                areaTotal.put(entry.getKey(), areaTotal.getOrDefault(entry.getKey(), 0L) + areaIdxTotal);
            }
        }
        return areaTotal;
    }

    public TableGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }
}

