package com.vegasnight.game.logserver;

import com.alibaba.fastjson.JSONObject;
import com.vegasnight.game.logdata.ILogService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 11
 * @date 2025/5/27 17:55
 */
@Service
@com.alibaba.dubbo.config.annotation.Service
public class LogService implements ILogService {
    private Logger log = LoggerFactory.getLogger("GAME_LOG");

    @Override
    public void log(JSONObject jsonObject) {
        log.info(jsonObject.toJSONString());
    }
}
