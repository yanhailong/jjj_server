package com.jjg.game.hall.casino.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.core.utils.RobotUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author lm
 * @date 2025/12/2 16:37
 */
@Component
public class CasinoLogger extends BaseLogger {
    /**
     * 我的赌场操作日志
     * @param playerId 玩家id
     * @param floorId 楼层
     * @param operationType 操作类型 1清理 2清理加速 3升级 4升级加速 5雇佣
     * @param machineType 机台类型
     * @param accelerationTime 加速时间
     * @param costItemMap 消耗道具
     * @param result 剩余数量
     * @param currenLv 当前等级
     */
    public void sendCasinoOperationLog(long playerId, int floorId, int operationType, int machineType, long accelerationTime
            , Map<Integer, Long> costItemMap, ItemOperationResult result, int currenLv) {
        try {
            if(RobotUtil.isRobot(playerId)){
                return;
            }

            JSONObject json = new JSONObject();
            json.put("playerId", playerId);
            json.put("floorId", floorId);
            json.put("machineType", machineType);
            json.put("operationType", operationType);
            json.put("accelerationTime", accelerationTime);
            json.put("costItemMap", objectMapper.writeValueAsString(costItemMap));
            json.put("result", objectMapper.writeValueAsString(result));
            json.put("currenLv", currenLv);
            json.put("functionType", 2);
            sendLog("function", null, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 发送机台奖励日志
     * @param playerId 玩家id
     * @param rewardsMap 奖励
     * @param result 奖励后数量
     */
    public void sendCasinoRewardsLog(long playerId, Map<Integer, Long> rewardsMap, ItemOperationResult result) {
        try {
            if(RobotUtil.isRobot(playerId)){
                return;
            }

            JSONObject json = new JSONObject();
            json.put("playerId", playerId);
            json.put("rewardsMap", objectMapper.writeValueAsString(rewardsMap));
            json.put("result", objectMapper.writeValueAsString(result));
            json.put("functionType", 3);
            sendLog("function", null, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
