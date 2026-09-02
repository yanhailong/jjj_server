package com.jjg.game.social.service;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.sampledata.bean.RobotCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SimRobotService implements ConfigExcelChangeListener {
    private final Logger log = LoggerFactory.getLogger(SimRobotService.class);

    private final String TABLE_NAME = "node:robot:bind";

    private final RobotUtil robotUtil;
    private final RedisTemplate redisTemplate;

    public SimRobotService(RobotUtil robotUtil, RedisTemplate redisTemplate) {
        this.robotUtil = robotUtil;
        this.redisTemplate = redisTemplate;
    }

    public boolean bind(long robotId, long playerId) {
        RobotCfg robotCfg = robotUtil.getRobotCfg(robotId);
        if (robotCfg == null) {
            return false;
        }
        redisTemplate.opsForHash().put(TABLE_NAME, robotCfg.getId(), playerId);
        return true;
    }

    public long queryPlayerId(int cfgId) {
        Object o = redisTemplate.opsForHash().get(TABLE_NAME, cfgId);
        if(o == null){
            return 0;
        }
        return Long.parseLong(o.toString());
    }

    public RobotCfg queryRobotCfg(long robotId) {
        return robotUtil.getRobotCfg(robotId);
    }
}
