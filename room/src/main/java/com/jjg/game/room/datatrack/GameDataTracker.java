package com.jjg.game.room.datatrack;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.SimplePlayerInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 游戏埋点收集器
 *
 * @author 2CL
 */
public class GameDataTracker {
    // 玩家的埋点数据
    private final HashMap<Object, Object> playerTrackData = new HashMap<>();

    // 房间的埋点数据
    private final HashMap<String, Object> gameTrackData = new HashMap<>();
    // 埋点日志
    private final RoomDataTrackLogger trackLogger;
    // 游戏日志topic
    private final String gameLogTopic;
    // 是否已经开始
    private boolean isStarted;
    // 基础游戏埋点数据
    private final HashMap<String, Object> baseGameInfo = new HashMap<>();

    public GameDataTracker(AbstractGameController<?, ?> gameController, RoomDataTrackLogger trackerLogger) {
        baseGameInfo.putAll(trackerLogger.buildBaseGameInfo(gameController));
        gameLogTopic = trackerLogger.gameLogTopicPrefix;
        this.trackLogger = trackerLogger;
    }

    /**
     * 埋点开始
     */
    public void start() {
        // 清理一次数据再开始
        clearRecData();
        isStarted = true;
    }

    /**
     * 添加玩家埋点日志数据
     */
    public void addPlayerLogData(Object gamePlayer, String logFieldName, Object logValue) {
        if (isStarted) {
            // 机器人不打日志
            if (gamePlayer instanceof GameRobotPlayer) {
                return;
            }
            if (!playerTrackData.containsKey(gamePlayer)) {
                playerTrackData.put(gamePlayer, new HashMap<>());
            }
            HashMap<String, Object> playerDataMap = (HashMap<String, Object>) playerTrackData.get(gamePlayer);
            playerDataMap.put(logFieldName, logValue);
        }
    }


    /**
     * 添加游戏中的埋点日志数据
     */
    public void addGameLogData(String logFieldName, Object logValue) {
        if (isStarted) {
            gameTrackData.put(logFieldName, logValue);
        }
    }


    /**
     * 获取埋点日志logger
     */
    public RoomDataTrackLogger getTrackLogger() {
        return trackLogger;
    }

    /**
     * 结束收集
     */
    public void finishedDataCollect() {
        isStarted = false;
    }

    /**
     * 发送玩家的埋点数据
     */
    public void flushDataLog(EDataTrackLogType dataTrackLogType) {
        HashMap<String, Object> tempTrackData = new HashMap<>();
        Map<String, HashMap<String, Object>> playerDataList = new HashMap<>();
        // 玩家埋点数据为空，退出
        if (playerTrackData.isEmpty()) {
            return;
        }
        // 玩家数据
        for (Map.Entry<Object, Object> entry : playerTrackData.entrySet()) {
            buildPlayerData(entry, playerDataList);
        }
        // 玩家日志数据
        tempTrackData.put("playerData", playerDataList);
        // 游戏日志数据
        tempTrackData.put("gameData", gameTrackData);
        // 游戏基础信息
        tempTrackData.putAll(baseGameInfo);
        // 订单ID
        tempTrackData.put("orderId", trackLogger.getSnowflake().nextId());
        // 添加发送时间
        tempTrackData.put("sendTime", System.currentTimeMillis());
        String gameLogTopicTmp = gameLogTopic + "_" + dataTrackLogType.name().toLowerCase();
        // 发送日志数据
        trackLogger.sendLog(gameLogTopicTmp, tempTrackData);
        // 给玩家记录的日志，在发送之后需要进行清除
        playerTrackData.clear();
    }


    public void buildPlayerData(Map.Entry<Object, Object> entry, Map<String, HashMap<String, Object>> playerDataList) {
        HashMap<String, Object> playerData = new HashMap<>();
        switch (entry.getKey()) {
            case GamePlayer gamePlayer -> {
                if (gamePlayer instanceof GameRobotPlayer) {
                    return;
                }
                playerData.put("playerInfo", trackLogger.buildGamePlayerInfo(gamePlayer));
                playerData.put("data", entry.getValue());
                playerDataList.put(gamePlayer.getId() + "", playerData);
            }
            case SimplePlayerInfo playerInfo -> {
                playerData.put("playerInfo", trackLogger.buildGamePlayerInfo(playerInfo));
                playerData.put("data", entry.getValue());
                playerDataList.put(playerInfo.playerId() + "", playerData);
            }
            default -> {
            }
        }
    }

    /**
     * 获取数据收集的值
     */
    public Object getDataTrackValue(GamePlayer gamePlayer) {
        return playerTrackData.get(gamePlayer);
    }

    /**
     * 发送批量的玩家埋点数据
     */
    public void flushDataLog(Collection<GamePlayer> gamePlayers, EDataTrackLogType dataTrackLogType) {
        for (GamePlayer gamePlayer : gamePlayers) {
            flushDataLog(dataTrackLogType);
        }
    }

    /**
     * 发送玩家的埋点数据后关闭收集
     */
    public void sendAndClose(EDataTrackLogType dataTrackLogType) {
        // 发送玩家数据
        flushDataLog(dataTrackLogType);
        // 完成数据收集
        finishedDataCollect();
    }

    /**
     * 发送批量的玩家埋点数据后关闭收集
     */
    public void sendAndClose(Collection<GamePlayer> gamePlayers, EDataTrackLogType dataTrackLogType) {
        // 发送玩家数据
        flushDataLog(gamePlayers, dataTrackLogType);
        // 完成数据收集
        finishedDataCollect();
    }

    /**
     * 发送退出游戏日志
     */
    public void sendExitGameLog(GamePlayer gamePlayer) {
        trackLogger.exitGame(gamePlayer, TimeHelper.nowInt() - gamePlayer.getEnterGameTime());
    }


    /**
     * 是否已经开始
     */
    public boolean isStarted() {
        return isStarted;
    }

    /**
     * 关闭数据收集
     */
    public void shutdownDataTracker() {
        // 暂停收集
        finishedDataCollect();
        // 清除收集数据
        clearRecData();
        // 清除房间基础数据
        baseGameInfo.clear();
    }

    /**
     * 清理记录数据
     */
    private void clearRecData() {
        playerTrackData.clear();
        gameTrackData.clear();
    }
}
