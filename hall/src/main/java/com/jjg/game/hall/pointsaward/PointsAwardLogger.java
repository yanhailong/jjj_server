package com.jjg.game.hall.pointsaward;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.logger.BaseLogger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 积分大奖日志
 */
@Component
public class PointsAwardLogger extends BaseLogger {

    /**
     * 积分增加日志
     *
     * @param playerId 玩家id
     * @param points   积分
     * @param type     类型
     */
    public void pointsChangeLog(long playerId, int points, int type, boolean flag, long resultValue) {
        JSONObject log = new JSONObject();
        log.put("playerId", playerId);
        log.put("points", points);
        log.put("type", type);
        //true + false -
        log.put("flag", flag);
        //更新后的值
        log.put("afterValue", resultValue);
        log.put("time", LocalDateTime.now());
        sendLog("pointsAwardPointsChange", null, log);
    }



}
