package com.jjg.game.core.service;

/**
 * @author lm
 * @date 2025/7/10 17:06
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.GameStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameStatusService {

    private static final Logger log = LoggerFactory.getLogger(GameStatusService.class);
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getGameStatusField(int gameId) {
        return "game:" + gameId;
    }

    /**
     * 保存或更新游戏状态
     */
    public boolean saveOrUpdateGameStatus(GameStatus gameStatus) {
        try {
            String field = getGameStatusField(gameStatus.gameId());
            String value = objectMapper.writeValueAsString(gameStatus);
            redisTemplate.opsForHash().put(GameConstant.Redis.GAME_STATUS_KEY, field, value);
            return true;
        } catch (JsonProcessingException e) {
            log.error("序列化游戏状态失败", e);
        }
        return false;
    }


    /**
     * 获取所有游戏状态
     */
    public List<GameStatus> getAllGameStatus() {
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        Map<String, String> entries = hashOps.entries(GameConstant.Redis.GAME_STATUS_KEY);

        return entries.values().stream()
                .map(value -> {
                    try {
                        return objectMapper.readValue(value, GameStatus.class);
                    } catch (IOException e) {
                        throw new RuntimeException("反序列化游戏状态失败", e);
                    }
                })
                .collect(Collectors.toList());
    }

}
