package com.jjg.game.account.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.logger.CoreLogger;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/28 11:40
 */
@Component
public class AccountLogger extends CoreLogger {

    private final String LOG_REGISTER_TYPE = "register";

    /**
     * 注册
     * @param account
     * @param registerType
     * @param playerId
     * @return
     */
    public void register(String account,int registerType,long playerId) {
        try{
            JSONObject json = new JSONObject();
            json.put("logtype", LOG_REGISTER_TYPE);
            json.put("registerType",registerType);
            json.put("account",account);
            json.put("playerId",playerId);
            sendLog(json);
        }catch (Exception e){
            log.error("记录guestLogin登录日志异常",e);
        }
    }
}
