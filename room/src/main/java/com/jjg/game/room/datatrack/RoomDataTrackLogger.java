package com.jjg.game.room.datatrack;

import cn.hutool.core.lang.Snowflake;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.core.manager.SnowflakeManager;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.SimplePlayerInfo;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 游戏数据埋点logger
 *
 * @author 2CL
 */
@Component
public class RoomDataTrackLogger extends BaseLogger {

    @Autowired
    protected NodeConfig nodeConfig;
    // 雪花算法
//    protected Snowflake snowflake = new Snowflake(NodeType.GAME.getValue());
    @Autowired
    protected SnowflakeManager snowflakeManager;
    // 游戏日志topic前缀
    protected final String gameLogTopicPrefix = "game_bet";

    /**
     * 构建基础游戏信息
     */
    public Map<String, Object> buildBaseGameInfo(AbstractGameController<?, ?> gameController) {
        HashMap<String, Object> baseGameInfo = new HashMap<>();
        baseGameInfo.put("nodeName", nodeConfig.getName());
        baseGameInfo.put("gameId", gameController.getGameDataVo().getRoomCfg().getGameID());
        baseGameInfo.put("gameCfgId", gameController.getGameDataVo().getRoomCfg().getId());
        baseGameInfo.put("roomId", gameController.getRoom().getId());
        return baseGameInfo;
    }

    /**
     * 构建基础玩家信息
     */
    public Map<String, Object> buildGamePlayerInfo(GamePlayer gamePlayer) {
        HashMap<String, Object> baseGameInfo = new HashMap<>();
        baseGameInfo.put("playerId", gamePlayer.getId());
        baseGameInfo.put("playerName", gamePlayer.getNickName());
        return baseGameInfo;
    }

    /**
     * 构建基础玩家信息
     */
    public Map<String, Object> buildGamePlayerInfo(SimplePlayerInfo gamePlayer) {
        HashMap<String, Object> baseGameInfo = new HashMap<>();
        baseGameInfo.put("playerId", gamePlayer.playerId());
        baseGameInfo.put("playerName", gamePlayer.name());
        return baseGameInfo;
    }

    /**
     * 根据topic发送日志
     */
    public void sendLog(String topic, Map<String, Object> data) {
        String sendData = JSON.toJSONString(data, SerializerFeature.WriteNonStringKeyAsString);
        log.debug("发送日志数据：{} {}", topic, sendData);
        kafkaTemplate.send(topic, sendData);
    }

    public Snowflake getSnowflake() {
        return snowflakeManager.getSnowflake();
    }
}
