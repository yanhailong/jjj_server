package com.jjg.game.hall.pointsaward;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.core.utils.RobotUtil;
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
            if(RobotUtil.isRobot(playerId)){
                return;
            }

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
    public void signInLog(Player player, int signCount, int points, long afterValue) {
        try {
            if(RobotUtil.isRobot(player.getId())){
                return;
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("signCount", signCount);
            jsonObject.put("points", points);
            jsonObject.put("afterValue", afterValue);
            sendLog("pointsAwardSignIn", player, jsonObject);
        } catch (Exception e) {
            log.error("记录签到日志错误!", e);
        }
    }

    public void turntableLog(long playerId,int beforeCount, int changeCount, int afterCount, int consumePoints, int getPoints, long afterValue) {
        turntableLog(playerId, beforeCount, changeCount, afterCount, consumePoints, getPoints, afterValue, "");
    }

    /**
     * 转盘抽奖日志
     *
     */
    public void turntableLog(long playerId,int beforeCount, int changeCount, int afterCount, int consumePoints, int getPoints, long afterValue, String orderId) {
        try {
            if(RobotUtil.isRobot(playerId)){
                return;
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("consumePoints", consumePoints);
            jsonObject.put("getPoints", getPoints);
            jsonObject.put("afterValue", afterValue);
            jsonObject.put("beforeCount", beforeCount);
            jsonObject.put("changeCount", changeCount);
            jsonObject.put("afterCount", afterCount);
            jsonObject.put("playerId", playerId);
            jsonObject.put("orderId", orderId);
            sendLog("pointsAwardTurntable", null, jsonObject);
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
            jsonObject.put("data", JSON.toJSONString(data));
            sendLog("pointsAwardLeaderboard", null, jsonObject);
        } catch (Exception e) {
            log.error("记录排行榜历史记录日志错误!", e);
        }
    }

    /**
     * 领取阶段奖励
     *
     * @param playerId
     * @param points
     * @param changeGold
     * @param afterGold
     * @param autoRecive
     */
    public void ladderReward(long playerId, long points, long changeGold, long afterGold, boolean autoRecive) {
        try {
            JSONObject json = new JSONObject();
            json.put("points", points); //积分挡位
            json.put("changeGold", changeGold);  //获得金币
            json.put("afterGold", afterGold);
            json.put("autoRecive", autoRecive);  //自动领取
            json.put("playerId", playerId);
            json.put("type", "time_process");
            sendLog("turntime", null, json);
        } catch (Exception e) {
            log.error("记录领取阶段奖励日志异常", e);
        }
    }

}
