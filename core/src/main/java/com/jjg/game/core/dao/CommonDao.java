package com.jjg.game.core.dao;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用dao
 */
@Repository
public class CommonDao {
    private Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private RedisTemplate redisTemplate;

    private final String COMMON_TABLE_NAME = "common";

    private Map<Integer, Object> map;

    public void init() {
        loadAll();
    }

    public void loadAll() {
        this.map = redisTemplate.opsForHash().entries(COMMON_TABLE_NAME);
        log.info("加载 common配置 map = {}", JSONObject.toJSONString(map));
    }

    public void setValue(int id, String value) {
        redisTemplate.opsForHash().put(COMMON_TABLE_NAME, id, value);
    }

    public String getStrValue(int id) {
        if (this.map == null || this.map.isEmpty()) {
            return null;
        }
        return (String) map.get(id);
    }
}
