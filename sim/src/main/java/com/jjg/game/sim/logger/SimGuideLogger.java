package com.jjg.game.sim.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.logger.BaseLogger;
import org.springframework.stereotype.Component;

import java.util.List;

/** 模拟经营引导后台操作 Kafka 日志。 */
@Component
public class SimGuideLogger extends BaseLogger {
    /** BaseLogger 会将 Topic 统一转换为小写。后台 GM 操作结果。 */
    public static final String OPERATION_TOPIC = "simGuideOperation";
    /** 玩家正常完成具体引导步骤。 */
    public static final String PLAYER_COMPLETE_TOPIC = "simGuideComplete";

    public static final int OPERATION_FINISH_ALL = 1;
    public static final int OPERATION_FINISH_SPECIFIED = 2;
    public static final int STATUS_COMPLETED = 1;

    /**
     * 游戏服完成后台引导操作后发送处理结果。
     * 指定引导操作才携带 guideIds，全部完成操作不发送该字段。
     */
    public void completed(long playerId, int operationType, List<Integer> guideIds) {
        JSONObject json = new JSONObject();
        json.put("playerId", playerId);
        json.put("operationType", operationType);
        if (operationType == OPERATION_FINISH_SPECIFIED) {
            json.put("guideIds", guideIds);
        }
        json.put("status", STATUS_COMPLETED);
        json.put("operationTime", System.currentTimeMillis());
        sendLog(OPERATION_TOPIC, null, json);
    }

    /**
     * 玩家第一次完成具体引导步骤后发送。
     * 消息时间由 BaseLogger 统一写入 time 字段。
     */
    public void playerCompleted(long playerId, String playerName, int guideId) {
        JSONObject json = new JSONObject();
        json.put("playerId", playerId);
        json.put("playerName", playerName);
        json.put("guideId", guideId);
        json.put("status", STATUS_COMPLETED);
        sendLog(PLAYER_COMPLETE_TOPIC, null, json);
    }
}
