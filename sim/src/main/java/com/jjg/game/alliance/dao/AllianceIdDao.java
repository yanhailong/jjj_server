package com.jjg.game.alliance.dao;

import com.jjg.game.alliance.constant.AllianceConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2026/6/17
 */
@Repository
public class AllianceIdDao {
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 初始化id
     */
    public void init() {
        redisTemplate.opsForValue().setIfAbsent(AllianceConst.RedisKey.ID_SEQ, 10000);
    }

    public long nextAllianceId() {
        return redisTemplate.opsForValue().increment(AllianceConst.RedisKey.ID_SEQ, 1);
    }
}
