package com.jjg.game.account.dao;

import com.jjg.game.account.config.AccountConfig;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.utils.RobotUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 用于生成或者获取playerId
 *
 * @author 11
 * @date 2025/5/26 9:45
 */
@Repository
public class PlayerIdDao {
    private static final String tableName = "playerIncrId:";

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private AccountConfig accountConfig;
    @Autowired
    private RobotUtil robotUtil;

    /**
     * 初始化id
     */
    public void init() {
        redisTemplate.opsForValue().setIfAbsent(tableName, accountConfig.getPlayerBeginId());
        robotUtil.initRobotStartId(accountConfig.getPlayerBeginId());
    }

    /**
     * 获取一个新的playerId
     */
    public long getNewId() {
        // 最大尝试10次，获取新的ID
        long newId = 0, maxTry = 10;
        // 排除机器人ID
        while (maxTry-- > 0) {
            newId = redisTemplate.opsForValue().increment(tableName, 1);
            if (newId % GameConstant.ROBOT_ID_PRIME_NUMBER != 0) {
                break;
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
        }
        return newId;
    }
}
