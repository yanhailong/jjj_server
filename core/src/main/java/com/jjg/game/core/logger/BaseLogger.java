package com.jjg.game.core.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.core.data.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author 11
 * @date 2025/6/19 9:58
 */
public class BaseLogger {
    @Autowired
    protected NodeConfig nodeConfig;
    @Autowired
    protected KafkaTemplate<String, String> kafkaTemplate;

    private final String GAME_LOGS_TOPIC = "game-logs";


    protected Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 在线统计
     */
    public void online(int num, String serverIp) {
        try {
            JSONObject json = new JSONObject();
            json.put("logType", "online");
            json.put("num", num);
            json.put("serverIp", serverIp);
            sendLog(null, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }


    public void gmOrder(String order, Long playerId, String result) {
        try {
            JSONObject json = new JSONObject();
            json.put("logType", "gm");
            json.put("order", order);
            if (playerId != null) {
                json.put("playerId", playerId);
            }
            json.put("result", result);
            sendLog(null, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }


    /**
     * 金币变化
     *
     * @param player
     * @param gold
     * @param addType
     */
    public void useMoney(Player player, long beforeGold, long gold, String addType, String desc) {
        try {
            JSONObject json = new JSONObject();
            json.put("logType", "goldChange");
            json.put("beforeGold", beforeGold);
            json.put("gold", gold);
            json.put("afterGold", player.getGold());
            json.put("addType", addType);
            json.put("desc", desc);

            sendLog(player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * vip等级变化
     *
     * @param player
     * @param beforeLevel
     * @param vipLevel
     * @param addType
     */
    public void vip(Player player, int beforeLevel, int vipLevel, String addType, String desc) {
        try {
            JSONObject json = new JSONObject();
            json.put("logType", "vipLevelChange");
            json.put("beforeLevel", beforeLevel);
            json.put("currentLevel", vipLevel);
            json.put("addType", addType);
            json.put("desc", desc);
            sendLog(player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 进入游戏
     *
     * @param player
     * @param gameType
     * @return
     */
    public void enterGame(Player player, int gameType, int roomCfgId) {
        try {
            JSONObject json = new JSONObject();
            json.put("logType", "enterGame");
            json.put("gameType", gameType);
            json.put("roomCfgId", roomCfgId);
            sendLog(player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 退出游戏
     *
     * @param player
     * @param gameType
     * @return
     */
    public void exitGame(Player player, int gameType) {
        try {
            JSONObject json = new JSONObject();
            json.put("logType", "exitGame");
            json.put("gameType", gameType);
            sendLog(player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    protected void sendLog(Player player, JSONObject json) {
        if (player != null) {
            json.put("playerId", player.getId());
        }

        json.put("time", System.currentTimeMillis());
        json.put("nodeName", nodeConfig.getName());
        kafkaTemplate.send(GAME_LOGS_TOPIC, JSONObject.toJSONString(json));
    }

    protected void sendLog(JSONObject json) {
        sendLog(null, json);
    }
}
