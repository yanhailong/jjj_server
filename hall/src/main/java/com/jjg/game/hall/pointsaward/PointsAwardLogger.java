package com.jjg.game.hall.pointsaward;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardData;
import com.jjg.game.hall.service.HallPlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 积分大奖日志
 */
@Component
public class PointsAwardLogger extends BaseLogger {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final HallPlayerService hallPlayerService;

    public PointsAwardLogger(HallPlayerService hallPlayerService) {
        this.hallPlayerService = hallPlayerService;
    }

    /**
     * 积分增加日志
     *
     * @param playerId 玩家id
     * @param points   积分
     * @param type     类型
     */
    public void pointsChangeLog(long playerId, int points, int type, boolean flag, long afterValue) {
        try {
            Player player = hallPlayerService.get(playerId);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("points", points);
            jsonObject.put("type", type);
            //true + false -
            jsonObject.put("flag", flag);
            //更新后的值
            jsonObject.put("afterValue", afterValue);
            sendLog("pointsAwardPointsChange", player, jsonObject);
        } catch (Exception e) {
            log.error("积分变化日志错误!", e);
        }
    }

    /**
     * 签到日志
     *
     * @param signCount  签到总天数
     * @param points     本次获得积分
     * @param afterValue 当前总积分
     */
    public void signInLog(long playerId, int signCount, int points, long afterValue) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("signCount", signCount);
            jsonObject.put("points", points);
            jsonObject.put("afterValue", afterValue);
            Player player = hallPlayerService.get(playerId);
            sendLog("pointsAwardSignIn", player, jsonObject);
        } catch (Exception e) {
            log.error("记录签到日志错误!", e);
        }
    }

    /**
     * 转盘抽奖日志
     *
     * @param consumePoints 消耗的积分
     * @param getPoints     获取的积分
     * @param afterValue    抽奖后玩家当前积分
     */
    public void turntableLog(long playerId, int consumePoints, int getPoints, long afterValue) {
        try {
            Player player = hallPlayerService.get(playerId);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("consumePoints", consumePoints);
            jsonObject.put("getPoints", getPoints);
            jsonObject.put("afterValue", afterValue);
            sendLog("pointsAwardTurntable", player, jsonObject);
        } catch (Exception e) {
            log.error("记录转盘日志错误!", e);
        }
    }

    /**
     * 记录排行榜历史记录
     */
    public void addLeaderboardHistory(PointsAwardLeaderboardData data) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("data", data);
            sendLog("pointsAwardLeaderboard", null, jsonObject);
        } catch (Exception e) {
            log.error("记录排行榜历史记录日志错误!", e);
        }
    }

}
