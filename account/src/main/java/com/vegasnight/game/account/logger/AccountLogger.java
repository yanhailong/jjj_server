package com.vegasnight.game.account.logger;

import com.alibaba.fastjson.JSONObject;
import com.vegasnight.game.core.data.Player;
import com.vegasnight.game.core.logger.CoreLogger;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/28 11:40
 */
@Component
public class AccountLogger extends CoreLogger {
    /**
     * 登录日志
     * @param guest
     * @return
     */
    public JSONObject test(String guest){
        JSONObject json = new JSONObject();
        try{
            json.put("guest",guest);
        }catch (Exception e){
            log.error("记录login登录日志异常",e);
        }
        return json;
    }
}
