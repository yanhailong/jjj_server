package com.jjg.game.hall.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import org.springframework.stereotype.Component;

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
    public void login(Player player, String token, int loginType, int channel, String ip, int device) {
        try {
            JSONObject json = new JSONObject();
//            json.put("logType", "login");
            json.put("loginType", loginType);
            json.put("token", token);
            json.put("channel", channel);
            json.put("ip", ip);
            json.put("device", device);
            sendLog("login", player, json);
        } catch (Exception e) {
            log.error("记录login登录日志异常", e);
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
}
