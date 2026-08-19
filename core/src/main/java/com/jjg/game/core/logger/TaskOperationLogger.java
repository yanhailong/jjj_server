package com.jjg.game.core.logger;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import java.util.List;

/** 后台完成通用任务 Kafka 日志。 */
@Component
public class TaskOperationLogger extends BaseLogger {
    public static final String TOPIC = "taskOperation";

    public void completed(long playerId, int operationType, List<Integer> taskIds) {
        JSONObject json = new JSONObject();
        json.put("playerId", playerId);
        json.put("operationType", operationType);
        if (operationType == 2) json.put("taskIds", taskIds);
        json.put("status", 1);
        json.put("operationTime", System.currentTimeMillis());
        sendLog(TOPIC, null, json);
    }
}
