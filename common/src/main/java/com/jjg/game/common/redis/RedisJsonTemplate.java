package com.jjg.game.common.redis;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author lm
 * @date 2025/8/18 11:09
 */

@Component
public class RedisJsonTemplate {

    private  final Logger log = LoggerFactory.getLogger(RedisJsonTemplate.class);
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisJsonTemplate(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 全量保存对象
     */
    public <T> void set(String key, T value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.execute((RedisCallback<Object>) connection ->
                    connection.execute(
                            "JSON.SET",
                            key.getBytes(),
                            "$".getBytes(),
                            json.getBytes()
                    )
            );
        } catch (JsonProcessingException e) {
            log.error("Redis JSON序列化失败", e);
        }
    }

    /**
     * 获取整个对象
     */
    public <T> T get(String key, TypeReference<T> typeRef) {
        byte[] rawJson = (byte[]) redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.execute(
                        "JSON.GET",
                        key.getBytes(),
                        "$".getBytes()
                ));
        if (rawJson == null) return null;

        try {
            // RedisJSON JSON.GET 默认返回数组，需要取第一个元素
            String jsonArray = new String(rawJson);
            return objectMapper.readValue(objectMapper.readTree(jsonArray).get(0).toString(), typeRef);
        } catch (JsonProcessingException e) {
            log.error("Redis JSON反序列化失败", e);
        }
        return null;
    }

    /**
     * 局部更新字段
     */
    public <T> void setPath(String key, String path, T value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.execute((RedisCallback<Object>) connection ->
                    connection.execute(
                            "JSON.SET",
                            key.getBytes(),
                            path.getBytes(),
                            json.getBytes()
                    )
            );
        } catch (JsonProcessingException e) {
            log.error("Redis JSON局部序列化失败", e);
        }
    }

    /**
     * 获取局部字段
     */
    public <T> T getPath(String key, String path, TypeReference<T> typeRef) {
        byte[] rawJson = (byte[]) redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.execute(
                        "JSON.GET",
                        key.getBytes(),
                        path.getBytes()
                ));
        if (rawJson == null) return null;

        try {
            return objectMapper.readValue(rawJson, typeRef);
        } catch (IOException e) {
            log.error("Redis JSON局部反序列化失败", e);
        }
        return null;
    }

    /**
     * 删除字段或整个对象
     */
    public void delete(String key, String path) {
        redisTemplate.execute((RedisCallback<Object>) connection ->
                connection.execute(
                        "JSON.DEL",
                        key.getBytes(),
                        path.getBytes()
                )
        );
    }
}

