package com.jjg.game.sim.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.logger.BaseLogger;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** 模拟经营主线任务生命周期 Kafka 日志。 */
@Component
public class SimMainTaskLogger extends BaseLogger {
    public static final String TOPIC = "simMainLineTask";

    public static final int EVENT_ACTIVATED = 1;
    public static final int EVENT_COMPLETED = 2;
    public static final int EVENT_REWARDED = 3;
    public static final int EVENT_ALL_COMPLETED = 4;

    public void activated(long playerId, String playerName, int taskId,
                          int previousTaskId, int nextTaskId, long createTime) {
        JSONObject json = base(playerId, playerName, taskId, EVENT_ACTIVATED,
                TaskConstant.TaskStatus.STATUS_IN_PROGRESS);
        json.put("previousTaskId", previousTaskId);
        json.put("nextTaskId", nextTaskId);
        json.put("createTime", createTime);
        sendLog(TOPIC, null, json);
    }

    public void completed(long playerId, String playerName, int taskId,
                          int conditionId, long progress, long target, long completeTime) {
        JSONObject json = base(playerId, playerName, taskId, EVENT_COMPLETED,
                TaskConstant.TaskStatus.STATUS_COMPLETED);
        json.put("conditionId", conditionId);
        json.put("progress", progress);
        json.put("target", target);
        json.put("completeTime", completeTime);
        sendLog(TOPIC, null, json);
    }

    public void rewarded(long playerId, String playerName, int taskId,
                         List<Item> rewards, int nextTaskId, long rewardTime) {
        JSONObject json = base(playerId, playerName, taskId, EVENT_REWARDED,
                TaskConstant.TaskStatus.STATUS_REWARDED);
        json.put("rewards", rewards == null ? Collections.emptyList() : rewards);
        json.put("nextTaskId", nextTaskId);
        json.put("rewardTime", rewardTime);
        sendLog(TOPIC, null, json);
    }

    public void allCompleted(long playerId, String playerName, int taskId, long rewardTime) {
        JSONObject json = base(playerId, playerName, taskId, EVENT_ALL_COMPLETED,
                TaskConstant.TaskStatus.STATUS_REWARDED);
        json.put("rewardTime", rewardTime);
        sendLog(TOPIC, null, json);
    }

    private JSONObject base(long playerId, String playerName, int taskId, int eventType, int status) {
        JSONObject json = new JSONObject();
        json.put("playerId", playerId);
        json.put("playerName", playerName == null ? "" : playerName);
        json.put("taskId", taskId);
        json.put("taskType", TaskConstant.TaskType.MAIN_LINE);
        json.put("eventType", eventType);
        json.put("status", status);
        return json;
    }
}
