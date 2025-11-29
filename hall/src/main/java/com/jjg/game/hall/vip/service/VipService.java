package com.jjg.game.hall.vip.service;

import com.jjg.game.hall.vip.dao.VipDao;
import com.jjg.game.hall.vip.data.Vip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author lm
 * @date 2025/8/7 15:16
 */
@Service
public class VipService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final String tableName = "vip:";
    private final RedisTemplate<String, Vip> redisTemplate;
    private final VipDao vipDao;

    public VipService(@Autowired RedisTemplate<String, Vip> redisTemplate,
                      @Autowired VipDao vipDao) {
        this.redisTemplate = redisTemplate;
        this.vipDao = vipDao;
    }

    /**
     * 查询 VIP 对象
     * 先查询redis
     * 再查询mongodb
     *
     * @param playerId 玩家id
     * @return 玩家Vip数据
     */
    public Optional<Vip> getFromAllDB(long playerId) {
        Optional<Vip> vipData = redisGet(playerId);
        if (vipData.isPresent()) {
            return vipData;
        }
        return vipDao.findById(playerId);
    }

    /**
     * 持久化到mongodb
     *
     * @param playerId 玩家id
     */
    public void moveToMongo(long playerId) {
        try {
            Optional<Vip> vipOptional = getFromAllDB(playerId);
            if (vipOptional.isEmpty()) {
                return;
            }
            vipDao.save(vipOptional.get());
            redisDel(playerId);
        } catch (Exception e) {
            log.error("保存到mongo失败 playerId:{}", playerId, e);
        }
    }

    private String getKey(long playerId) {
        return tableName + playerId;
    }


    /**
     * 保存整个对象
     */
    public void redisSave(long playerId, Vip vip) {
        redisTemplate.opsForValue().set(getKey(playerId), vip);
    }


    /**
     * 删除整个对象
     */
    public void redisDel(long playerId) {
        redisTemplate.delete(getKey(playerId));
    }


    /**
     * 通过玩家ID获取玩家的VIP信息
     *
     * @param playerId 玩家ID
     * @return vip信息
     */
    public Optional<Vip> redisGet(long playerId) {
        ValueOperations<String, Vip> opsForValue = redisTemplate.opsForValue();
        return Optional.ofNullable(opsForValue.get(getKey(playerId)));
    }


}
