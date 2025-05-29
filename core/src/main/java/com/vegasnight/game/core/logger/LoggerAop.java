package com.vegasnight.game.core.logger;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.vegasnight.game.common.config.NodeConfig;
import com.vegasnight.game.common.timer.TimerCenter;
import com.vegasnight.game.common.timer.TimerEvent;
import com.vegasnight.game.common.timer.TimerListener;
import com.vegasnight.game.common.utils.RandomUtils;
import com.vegasnight.game.core.data.Player;
import com.vegasnight.game.logdata.ILogService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/5/28 9:35
 */
@Component
@Aspect
public class LoggerAop implements TimerListener {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @DubboReference
    private ILogService logService;
    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private TimerCenter timerCenter;


    @AfterReturning(value = "execution(* com.vegasnight.game.*.logger.*Logger.*(..))", returning = "keys")
    public void afterReturning(JoinPoint joinPoint, JSONObject keys) {
        try {
            if (keys == null) {
                return;
            }
            keys.put("id", RandomUtils.getUUid());
            keys.put("time", System.currentTimeMillis());
            keys.put("serverId", nodeConfig.getName());

            Object[] params = joinPoint.getArgs();
            for (Object param : params) {
                if (param instanceof Player) {
                    Player player = (Player) param;
                    if (!keys.containsKey("playerId")) {
                        keys.put("playerId", player.getId());
                    }
//                    addGameBaseInfo(player.getGameBaseInfo(), keys);
                    continue;
                }

            }

            //JSONObject tmp = keys;

            TimerEvent<JSONObject> event = new TimerEvent<>(this,0,keys);
            timerCenter.add(event);
        } catch (Exception e) {
            log.error("日志生成异常", e);
        }
    }


    @Override
    public void onTimer(TimerEvent e) {
        try {
            logService.log((JSONObject) e.getParameter());
        } catch (Exception ex) {
            log.error("日志记录异常", ex);
        }
    }
}
