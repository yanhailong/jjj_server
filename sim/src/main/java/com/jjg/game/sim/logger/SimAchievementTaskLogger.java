package com.jjg.game.sim.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.logger.BaseLogger;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** 模拟经营独立成就任务生命周期 Kafka 日志。 */
@Component
public class SimAchievementTaskLogger extends BaseLogger {
    public static final String TOPIC = "simAchievementTask";

    public static final int EVENT_COMPLETED = 2;
    public static final int EVENT_REWARDED = 3;

    public void completed(long playerId, String playerName, int taskId, int badgeId,
                          int conditionId, long progress, long target, long completeTime) {
        JSONObject json = base(playerId, playerName, taskId, badgeId, EVENT_COMPLETED,
                TaskConstant.TaskStatus.STATUS_COMPLETED);
        json.put("conditionId", conditionId);
        json.put("progress", progress);
        json.put("target", target);
        json.put("completeTime", completeTime);
        sendLog(TOPIC, null, json);
    }

    public void rewarded(long playerId, String playerName, int taskId, int badgeId,
                         List<Item> rewards, long rewardTime) {
        JSONObject json = base(playerId, playerName, taskId, badgeId, EVENT_REWARDED,
                TaskConstant.TaskStatus.STATUS_REWARDED);
        json.put("rewards", rewards == null ? Collections.emptyList() : rewards);
        json.put("rewardTime", rewardTime);
        sendLog(TOPIC, null, json);
    }

    private JSONObject base(long playerId, String playerName, int taskId, int badgeId,
                            int eventType, int status) {
        JSONObject json = new JSONObject();
        json.put("playerId", playerId);
        json.put("playerName", playerName == null ? "" : playerName);
        json.put("taskId", taskId);
        json.put("taskType", TaskConstant.TaskType.ACHIEVEMENT);
        json.put("badgeId", badgeId);
        json.put("eventType", eventType);
        json.put("status", status);
        return json;
    }
}
