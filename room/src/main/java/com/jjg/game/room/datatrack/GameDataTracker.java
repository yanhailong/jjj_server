package com.jjg.game.room.datatrack;

import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;

import java.util.Collection;
import java.util.HashMap;

/**
 * 游戏埋点收集器
 *
 * @author 2CL
 */
public class GameDataTracker {
    // 玩家的埋点数据
    private final HashMap<String, Object> playerTrackData = new HashMap<>();
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
        gameLogTopic = trackerLogger.gameLogTopicPrefix + gameController.getGameDataVo().getRoomCfg().getGameID();
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
     * 添加埋点日志数据
     */
    public void addPlayerLogData(String logFieldName, Object logValue) {
        if (isStarted) {
            playerTrackData.put(logFieldName, logValue);
        }
    }


    /**
     * 添加埋点日志数据
     */
    public void addGameLogData(String logFieldName, Object logValue) {
        if (isStarted) {
            gameTrackData.put(logFieldName, logValue);
        }
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
    public void sendLogWithPlayer(GamePlayer gamePlayer) {
        if (gamePlayer instanceof GameRobotPlayer) {
            return;
        }
        HashMap<String, Object> tempTrackData = new HashMap<>();
        // 游戏的日志数据
        tempTrackData.putAll(playerTrackData);
        // 游戏的日志数据
        tempTrackData.putAll(gameTrackData);
        // 基础的游戏信息
        tempTrackData.putAll(baseGameInfo);
        // 玩家信息
        tempTrackData.putAll(trackLogger.buildGamePlayerInfo(gamePlayer));
        // 订单ID
        tempTrackData.put("orderId", trackLogger.getSnowflake().nextId());
        // 发送日志数据
        trackLogger.sendLog(gameLogTopic, tempTrackData);
        // 给玩家记录的日志，在发送之后需要进行清除
        playerTrackData.clear();
    }

    /**
     * 获取数据收集的值
     */
    public Object getDataTrackValue(String trackFieldName) {
        return playerTrackData.get(trackFieldName);
    }

    /**
     * 发送批量的玩家埋点数据
     */
    public void sendLogWithPlayer(Collection<GamePlayer> gamePlayers) {
        for (GamePlayer gamePlayer : gamePlayers) {
            sendLogWithPlayer(gamePlayer);
        }
    }

    /**
     * 发送玩家的埋点数据后关闭收集
     */
    public void sendAndClose(GamePlayer gamePlayer) {
        // 发送玩家数据
        sendLogWithPlayer(gamePlayer);
        // 完成数据收集
        finishedDataCollect();
    }

    /**
     * 发送批量的玩家埋点数据后关闭收集
     */
    public void sendAndClose(Collection<GamePlayer> gamePlayers) {
        // 发送玩家数据
        sendLogWithPlayer(gamePlayers);
        // 完成数据收集
        finishedDataCollect();
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
        clearRecData();
        baseGameInfo.clear();
        isStarted = false;
    }

    /**
     * 清理记录数据
     */
    private void clearRecData() {
        playerTrackData.clear();
        gameTrackData.clear();
    }
}
