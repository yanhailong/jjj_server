package com.jjg.game.room.base;

import cn.hutool.core.lang.hash.Hash;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;

import java.util.Collection;
import java.util.HashMap;

/**
 * 游戏埋点收集器
 *
 * @author 2CL
 */
public class GameDataTracker {
    // 埋点数据
    private final HashMap<String, Object> trackData = new HashMap<>();
    // 埋点日志
    private GameDataTrackLogger trackLogger;
    // 游戏日志topic
    private final String gameLogTopic;
    // 是否已经开始
    private boolean isStarted;
    // 基础游戏埋点数据
    private final HashMap<String, Object> baseGameInfo = new HashMap<>();

    public GameDataTracker(AbstractGameController<?, ?> gameController, GameDataTrackLogger trackerLogger) {
        baseGameInfo.putAll(trackerLogger.buildBaseGameInfo(gameController));
        gameLogTopic = trackLogger.gameLogTopicPrefix + gameController.getGameDataVo().getRoomCfg().getGameID();
        this.trackLogger = trackerLogger;
    }

    /**
     * 埋点开始
     */
    public void start() {
        trackData.clear();
        isStarted = true;
    }

    /**
     * 添加埋点日志数据
     */
    public void addLogData(String logFieldName, Object logValue) {
        if (isStarted) {
            trackData.put(logFieldName, logValue);
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
    public void sendPlayerLog(GamePlayer gamePlayer) {
        HashMap<String, Object> tempTrackData = new HashMap<>();
        // 游戏的日志数据
        tempTrackData.putAll(trackData);
        // 基础的游戏信息
        tempTrackData.putAll(baseGameInfo);
        // 玩家信息
        tempTrackData.putAll(trackLogger.buildGamePlayerInfo(gamePlayer));
        // 订单ID
        tempTrackData.put("orderId", trackLogger.getSnowflake().nextId());
        trackLogger.sendLog(gameLogTopic, tempTrackData);
    }


    /**
     * 发送批量的玩家埋点数据
     */
    public void sendPlayerLog(Collection<GamePlayer> gamePlayers) {
        for (GamePlayer gamePlayer : gamePlayers) {
            sendPlayerLog(gamePlayer);
        }
    }

    /**
     * 发送玩家的埋点数据后关闭收集
     */
    public void sendAndClose(GamePlayer gamePlayer) {
        // 发送玩家数据
        sendPlayerLog(gamePlayer);
        // 完成数据收集
        finishedDataCollect();
    }

    /**
     * 发送批量的玩家埋点数据后关闭收集
     */
    public void sendAndClose(Collection<GamePlayer> gamePlayers) {
        // 发送玩家数据
        sendPlayerLog(gamePlayers);
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
        trackData.clear();
        baseGameInfo.clear();
        isStarted = false;
    }
}
