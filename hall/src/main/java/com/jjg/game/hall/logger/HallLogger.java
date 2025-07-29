package com.jjg.game.hall.logger;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.logger.BaseLogger;
import com.jjg.game.core.logger.CoreLogger;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/10 14:31
 */
@Component
public class HallLogger extends BaseLogger {
    /**
     * 登录日志
     * @param player
     * @return
     */
    public void login(Player player,String token,int loginType){
        try{
            JSONObject json = new JSONObject();
//            json.put("logType", "login");
            json.put("loginType", loginType);
            json.put("token", token);
            sendLog("login",player,json);
        }catch (Exception e){
            log.error("记录login登录日志异常",e);
        }
    }



}
