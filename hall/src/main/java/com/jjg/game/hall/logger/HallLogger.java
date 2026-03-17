package com.jjg.game.hall.logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author 11
 * @date 2025/6/10 14:31
 */
@Component
public class HallLogger extends BaseLogger {
    /**
     * 登录日志
     *
     * @param player
     * @return
     */
    public void login(Player player, String token, int loginType, int channel, String ip, int device, String mac) {
        try {
            JSONObject json = new JSONObject();
//            json.put("logType", "login");
            json.put("loginType", loginType);
            json.put("token", token);
            json.put("channel", channel);
            json.put("ip", ip);
            json.put("device", device);
            json.put("mac", mac);
            json.put("subChannel", player.getSubChannel());
            sendLog("login", player, json);
        } catch (Exception e) {
            log.error("记录login登录日志异常", e);
        }
    }

    /**
     * 登出
     *
     * @param playerId
     * @return
     */
    public void logout(long playerId, int sessionCreateTime) {
        try {
            JSONObject json = new JSONObject();
            json.put("playerId", playerId);

            if (sessionCreateTime < 1) {
                json.put("online", 0);
            } else {
                json.put("online", TimeHelper.nowInt() - sessionCreateTime);
            }
            sendLog("logout", null, json);
        } catch (Exception e) {
            log.error("记录logout登录日志异常", e);
        }
    }

    public void bind(Player player, int type, String data) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", type);
            json.put("data", data);
            sendLog("bind", player, json);
        } catch (Exception e) {
            log.error("记录绑定日志异常", e);
        }
    }

    public void pool(Map<Integer, Long> pool) {
        try {
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
            for (Map.Entry<Integer, Long> en : pool.entrySet()) {
                JSONObject tmpJson = new JSONObject();
                tmpJson.put("roomCfgId", en.getKey());
                tmpJson.put("value", en.getValue());
                array.add(tmpJson);
            }
            json.put("data", array);
            sendLog("pool", null, json);
        } catch (Exception e) {
            log.error("记录绑定日志异常", e);
        }
    }

    /**
     * 礼包码日志
     *
     * @param player   玩家数据
     * @param redeemId 礼包id
     * @param rewards  奖励
     * @param result   奖励后数据
     * @param code     礼包码
     */
    public void sendRedeemLog(Player player, long redeemId, Map<Integer, Long> rewards, ItemOperationResult result, String code) {

        try {
            JSONObject json = new JSONObject();
            json.put("redeemId", redeemId);
            //奖励
            json.put("rewards", objectMapper.writeValueAsString(rewards));
            if (result != null) {
                json.put("rewardsItemNum", objectMapper.writeValueAsString(result));
            }
            json.put("code", code);
            sendLog("cdk", player, json);
        } catch (Exception e) {
            log.error("sendRedeemLog error:", e);
        }
    }
}
