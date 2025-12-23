package com.jjg.game.table.common.data;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_BetCfg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对战类游戏的内存常驻数据 Value Object
 *
 * @author 2CL
 */
public class TableGameDataVo extends GameDataVo<Room_BetCfg> {

    // 玩家下注数据 玩家ID：下注区域ID：下注金币列表
    protected final Map<Long, Map<Integer, List<Integer>>> playerBetInfo = new HashMap<>();

    public void clearRoundData(AbstractGameController<?, ?> gameController) {
        // 玩家的区域押注信息清除
        playerBetInfo.clear();
    }

    public Map<Integer, List<Integer>> getPlayerBetInfo(long playerId) {
        return playerBetInfo.get(playerId);
    }

    public Map<Long, Map<Integer, List<Integer>>> getPlayerBetInfo() {
        return playerBetInfo;
    }

    public Map<Long, Map<Integer, List<Integer>>> getRealPlayerBetInfo() {
        Map<Long, Map<Integer, List<Integer>>> bet = new HashMap<>();
        for (Map.Entry<Long, Map<Integer, List<Integer>>> entry : playerBetInfo.entrySet()) {
            GamePlayer gamePlayer = getGamePlayerMap().get(entry.getKey());
            if (gamePlayer instanceof GameRobotPlayer) {
                continue;
            }
            bet.put(gamePlayer.getId(), entry.getValue());
        }
        return bet;
    }

    public Map<Integer, Map<Long, Long>> getRealPlayerAreaBetInfo() {
        Map<Integer, Map<Long, Long>> areaBetmap = new HashMap<>();
        for (Map.Entry<Long, Map<Integer, List<Integer>>> entry : playerBetInfo.entrySet()) {
            GamePlayer gamePlayer = getGamePlayerMap().get(entry.getKey());
            if (gamePlayer instanceof GameRobotPlayer) {
                continue;
            }
            for (Map.Entry<Integer, List<Integer>> listEntry : entry.getValue().entrySet()) {
                Map<Long, Long> longLongMap = areaBetmap.computeIfAbsent(listEntry.getKey(), k -> new HashMap<>());
                longLongMap.put(entry.getKey(), listEntry.getValue().stream().mapToLong(v -> v).sum());
            }
        }
        return areaBetmap;
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

    public Map<Integer, Map<Long, List<Integer>>> getBetInfo() {
        Map<Integer, Map<Long, List<Integer>>> map = new HashMap<>();
        for (Map.Entry<Long, Map<Integer, List<Integer>>> entry : playerBetInfo.entrySet()) {
            for (Map.Entry<Integer, List<Integer>> listEntry : entry.getValue().entrySet()) {
                Map<Long, List<Integer>> playerBetInfo = map.computeIfAbsent(listEntry.getKey(), k -> new HashMap<>());
                playerBetInfo.put(entry.getKey(), listEntry.getValue());
            }
        }
        return map;
    }

    public TableGameDataVo(Room_BetCfg roomCfg) {
        super(roomCfg);
    }

    @Override
    public void reloadRoomCfg() {
        roomCfg = GameDataManager.getRoom_BetCfg(roomCfg.getId());
    }

    /**
     * 更新玩家操作时间
     */
    public void updatePlayerOperateTime(long playerId) {
        GamePlayer gamePlayer = gamePlayerMap.get(playerId);
        if (gamePlayer != null) {
            gamePlayer.getTableGameData().setPlayerLatestOperateTime(System.currentTimeMillis());
        }
    }
}

