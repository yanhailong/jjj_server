package com.jjg.game.sim.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.logger.BaseLogger;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/** 模拟经营成就任务生命周期 Kafka 日志。 */
@Component
public class SimAchievementTaskLogger extends BaseLogger {
    public static final String TOPIC = "simAchievementTask";

    public static final int EVENT_ACTIVATED = 1;
    public static final int EVENT_COMPLETED = 2;
    public static final int EVENT_REWARDED = 3;
    public static final int EVENT_ALL_COMPLETED = 4;

    public void activated(long playerId, String playerName, int taskId, int group,
                          int previousTaskId, int nextTaskId, long createTime) {
        JSONObject json = base(playerId, playerName, taskId, group, EVENT_ACTIVATED,
                TaskConstant.TaskStatus.STATUS_IN_PROGRESS);
        json.put("previousTaskId", previousTaskId);
        json.put("nextTaskId", nextTaskId);
        json.put("createTime", createTime);
        sendLog(TOPIC, null, json);
    }

    public void completed(long playerId, String playerName, int taskId, int group,
                          int conditionId, long progress, long target, long completeTime) {
        JSONObject json = base(playerId, playerName, taskId, group, EVENT_COMPLETED,
                TaskConstant.TaskStatus.STATUS_COMPLETED);
        json.put("conditionId", conditionId);
        json.put("progress", progress);
        json.put("target", target);
        json.put("completeTime", completeTime);
        sendLog(TOPIC, null, json);
    }

    public void rewarded(long playerId, String playerName, int taskId, int group,
                         List<Item> rewards, int nextTaskId, long rewardTime) {
        JSONObject json = base(playerId, playerName, taskId, group, EVENT_REWARDED,
                TaskConstant.TaskStatus.STATUS_REWARDED);
        json.put("rewards", rewards == null ? Collections.emptyList() : rewards);
        json.put("nextTaskId", nextTaskId);
        json.put("rewardTime", rewardTime);
        sendLog(TOPIC, null, json);
    }

    public void allCompleted(long playerId, String playerName, int taskId, int group, long rewardTime) {
        JSONObject json = base(playerId, playerName, taskId, group, EVENT_ALL_COMPLETED,
                TaskConstant.TaskStatus.STATUS_REWARDED);
        json.put("rewardTime", rewardTime);
        sendLog(TOPIC, null, json);
    }

    private JSONObject base(long playerId, String playerName, int taskId, int group,
                            int eventType, int status) {
        JSONObject json = new JSONObject();
        json.put("playerId", playerId);
        json.put("playerName", playerName == null ? "" : playerName);
        json.put("taskId", taskId);
        json.put("taskType", TaskConstant.TaskType.ACHIEVEMENT);
        json.put("group", group);
        json.put("eventType", eventType);
        json.put("status", status);
        return json;
    }
}
