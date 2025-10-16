package com.jjg.game.core.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.Item;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志
 */
@Component
public class TaskLogger extends BaseLogger {

    /**
     * 玩家领取任务
     *
     * @param playerId 玩家id
     * @param configId 任务配置id
     */
    public void receiveTask(long playerId, int configId) {
        JSONObject log = new JSONObject();
        log.put("playerId", playerId);
        log.put("taskConfigId", configId);
        log.put("time", LocalDateTime.now());
        sendLog("receiveTask", null, log);
    }

    /**
     * 任务完成
     */
    public void completeTask(long playerId, int configId) {
        JSONObject log = new JSONObject();
        log.put("playerId", playerId);
        log.put("taskConfigId", configId);
        log.put("time", LocalDateTime.now());
        sendLog("completeTask", null, log);
    }

    /**
     * 任务奖励领取
     */
    public void receiveTaskAward(long playerId, int configId, List<Item> itemList) {
        JSONObject log = new JSONObject();
        log.put("playerId", playerId);
        log.put("taskConfigId", configId);
        log.put("time", LocalDateTime.now());
        if (itemList != null) {
            log.put("item", itemList);
        }
        sendLog("receiveTaskAward", null, log);
    }

}
