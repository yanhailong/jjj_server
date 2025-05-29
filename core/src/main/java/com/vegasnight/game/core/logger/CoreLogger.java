package com.vegasnight.game.core.logger;

import com.alibaba.fastjson.JSONObject;
import com.vegasnight.game.common.config.NodeConfig;
import com.vegasnight.game.core.data.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/26 11:24
 */
@Component
public class CoreLogger {
    @Autowired
    private NodeConfig nodeConfig;


    protected Logger log = LoggerFactory.getLogger(this.getClass());


    /**
     * 登录日志
     * @param player
     * @return
     */
    public JSONObject login(Player player){
        JSONObject json = new JSONObject();
        try{
        }catch (Exception e){
            log.error("记录login登录日志异常",e);
        }
        return json;
    }

    /**
     * 登出日志
     * @param player
     * @return
     */
    public JSONObject logout(Player player){
        JSONObject json = new JSONObject();
        try{
        }catch (Exception e){
            log.error("记录logout登出日志异常",e);
        }
        return json;
    }

    /**
     * 在线统计
     * @param num
     */
    public JSONObject online( int num,String serverIp){
        JSONObject json = new JSONObject();
        try{
        }catch (Exception e){
            log.error("记录online异常",e);
        }
        return json;
    }
}
