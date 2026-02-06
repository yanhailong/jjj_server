package com.jjg.game.account.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.logger.BaseLogger;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/28 11:40
 */
@Component
public class AccountLogger extends BaseLogger {


    /**
     * 注册
     *
     * @param account
     * @param registerType
     * @param playerId
     * @return
     */
    public void register(String account, int registerType, long playerId, int channel, String ip, int device, String mac, String phoneType, String subChannel, String shareId, String phoneName) {
        try {
            JSONObject json = new JSONObject();
            json.put("registerType", registerType);
            json.put("account", account);
            json.put("playerId", playerId);
            json.put("channel", channel);
            json.put("ip", ip);
            json.put("mac", mac);
            json.put("device", device);
            json.put("phoneType", phoneType);
            json.put("intro_way", subChannel);
            json.put("shareId", shareId);
            json.put("phoneName", phoneName);
            sendLog("register", null, json);
        } catch (Exception e) {
            log.error("记录guestLogin登录日志异常", e);
        }
    }
}
