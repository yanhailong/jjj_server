package com.jjg.game.room.base;

import cn.hutool.core.lang.Snowflake;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
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
public class GameDataTrackLogger extends BaseLogger {

    @Autowired
    protected NodeConfig nodeConfig;
    // 雪花算法
    protected Snowflake snowflake = new Snowflake(NodeType.GAME.getValue());
    // 游戏日志topic前缀
    protected final String gameLogTopicPrefix = "game_log_";

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
        baseGameInfo.put("id", gamePlayer.getId());
        baseGameInfo.put("name", gamePlayer.getNickName());
        return baseGameInfo;
    }

    /**
     * 根据topic发送日志
     */
    public void sendLog(String topic, Map<String, Object> data) {
        kafkaTemplate.send(topic, JSON.toJSONString(data));
    }

    public Snowflake getSnowflake() {
        return snowflake;
    }
}
