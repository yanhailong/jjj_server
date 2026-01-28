package com.jjg.game.common.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.data.PlayerSnapshot;
import com.jjg.game.common.redis.PlayerKeyIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author lm
 * @date 2026/1/22 14:44
 */
@Component
public class PlayerSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(PlayerSnapshotService.class);
    private final RedisTemplate<String, String> redis;
    private final MongoTemplate mongo;
    private final static String LUA_SCRIPT = """
            local result = {}
            for i = 1, #KEYS do
                local k = KEYS[i]
                local p = string.find(k, "#")
                if p then
                    local hashKey = string.sub(k, 1, p - 1)
                    local field = string.sub(k, p + 1)
                    local v = redis.call("HGET", hashKey, field)
                    if v then
                        result[k] = v
                        redis.call("HDEL", hashKey, field)
                    end
                else
                    local v = redis.call("GET", k)
                    if v then
                        result[k] = v
                        redis.call("DEL", k)
                    end
                end
            end
            return cjson.encode(result)
            """;

    private final static String LUA_RESTORE_SCRIPT = """
            local indexKey = KEYS[1]
            for i = 2, #KEYS do
                local k = KEYS[i]
                local v = ARGV[i - 1]
            
                local p = string.find(k, "#")
                if p then
                    local hashKey = string.sub(k, 1, p - 1)
                    local field = string.sub(k, p + 1)
                    redis.call("HSET", hashKey, field, v)
                    redis.call("SADD", indexKey, k)
                else
                    redis.call("SET", k, v)
                    redis.call("SADD", indexKey, k)
                end
            end
            
            return 1
            """;

    public PlayerSnapshotService(RedisTemplate<String, String> redis,
                                 MongoTemplate mongo) {
        this.redis = redis;
        this.mongo = mongo;
    }

    public Map<String, String> dumpByLua(long playerId) {
        try {
            String indexKey = "player_keys:" + playerId;
            Set<String> keys = redis.opsForSet().members(indexKey);
            if (keys == null || keys.isEmpty()) {
                return Map.of();
            }

            DefaultRedisScript<String> script = new DefaultRedisScript<>();
            script.setResultType(String.class);
            script.setScriptText(LUA_SCRIPT);
            String json = redis.execute(script, new ArrayList<>(keys));
            redis.delete(indexKey); // 清影子索引
            return new ObjectMapper().readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("保存玩家数据到mongo失败 playerId:{} ", playerId);
        }
        return Map.of();
    }

    public void dumpToMongo(long playerId) {
        Map<String, String> data = dumpByLua(playerId);
        if (data.isEmpty()) return;

        PlayerSnapshot snap = new PlayerSnapshot();
        snap.setUid(playerId);
        snap.setKeys(data);
        snap.setUpdateTime(new Date());

        mongo.save(snap);
    }

    public void restore(long playerId) {
        PlayerSnapshot snap = mongo.findById(playerId, PlayerSnapshot.class);
        if (snap == null) return;
        restoreByLua(playerId, snap);
    }

    private void restoreByLua(long playerId, PlayerSnapshot snap) {
        if (snap == null || snap.getKeys().isEmpty()) return;

        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();

        keys.add("player_keys:" + playerId);

        snap.getKeys().forEach((k, v) -> {
            keys.add(k);
            args.add(v);
        });

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LUA_RESTORE_SCRIPT);
        script.setResultType(Long.class);

        redis.execute(script, keys, args.toArray());
    }

}
