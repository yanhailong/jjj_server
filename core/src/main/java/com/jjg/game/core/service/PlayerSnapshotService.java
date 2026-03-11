package com.jjg.game.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.core.data.PlayerSnapshot;
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
                local prefix = string.sub(k, 1, 2)
                if prefix == "L:" then
                    local listKey = string.sub(k, 3)
                    local v = redis.call("LRANGE", listKey, 0, -1)
                    if v and #v > 0 then
                        result[k] = cjson.encode(v)
                    end
                elseif prefix == "S:" then
                    local setKeyWithMember = string.sub(k, 3)
                    local p = string.find(setKeyWithMember, "#")
                    if p then
                        local setKey = string.sub(setKeyWithMember, 1, p - 1)
                        local member = string.sub(setKeyWithMember, p + 1)
                        local exists = redis.call("SISMEMBER", setKey, member)
                        if exists == 1 then
                            result[k] = "1"
                        end
                    else
                        local v = redis.call("SMEMBERS", setKeyWithMember)
                        if v and #v > 0 then
                            result[k] = cjson.encode(v)
                        end
                    end
                elseif prefix == "Z:" then
                    local zsetKeyWithMember = string.sub(k, 3)
                    local p = string.find(zsetKeyWithMember, "#")
                    if p then
                        local zsetKey = string.sub(zsetKeyWithMember, 1, p - 1)
                        local member = string.sub(zsetKeyWithMember, p + 1)
                        local score = redis.call("ZSCORE", zsetKey, member)
                        if score then
                            result[k] = score
                        end
                    else
                        local v = redis.call("ZRANGE", zsetKeyWithMember, 0, -1, "WITHSCORES")
                        if v and #v > 0 then
                            result[k] = cjson.encode(v)
                        end
                    end
                elseif prefix == "H:" then
                    local hashKeyWithField = string.sub(k, 3)
                    local p = string.find(hashKeyWithField, "#")
                    if p then
                        local hashKey = string.sub(hashKeyWithField, 1, p - 1)
                        local field = string.sub(hashKeyWithField, p + 1)
                        local v = redis.call("HGET", hashKey, field)
                        if v then
                            result[k] = v
                        end
                    else
                        local v = redis.call("HGETALL", hashKeyWithField)
                        if v and #v > 0 then
                            result[k] = cjson.encode(v)
                        end
                    end
                else
                    local v = redis.call("GET", k)
                    if v then
                        result[k] = v
                    end
                end
            end
            return cjson.encode(result)
            """;

    private final static String LUA_DELETE_SCRIPT = """
            local indexKey = KEYS[1]
            for i = 2, #KEYS do
                local k = KEYS[i]
                local prefix = string.sub(k, 1, 2)
                if prefix == "L:" then
                    local listKey = string.sub(k, 3)
                    redis.call("DEL", listKey)
                elseif prefix == "S:" then
                    local setKeyWithMember = string.sub(k, 3)
                    local p = string.find(setKeyWithMember, "#")
                    if p then
                        local setKey = string.sub(setKeyWithMember, 1, p - 1)
                        local member = string.sub(setKeyWithMember, p + 1)
                        redis.call("SREM", setKey, member)
                    else
                        redis.call("DEL", setKeyWithMember)
                    end
                elseif prefix == "Z:" then
                    local zsetKeyWithMember = string.sub(k, 3)
                    local p = string.find(zsetKeyWithMember, "#")
                    if p then
                        local zsetKey = string.sub(zsetKeyWithMember, 1, p - 1)
                        local member = string.sub(zsetKeyWithMember, p + 1)
                        redis.call("ZREM", zsetKey, member)
                    else
                        redis.call("DEL", zsetKeyWithMember)
                    end
                elseif prefix == "H:" then
                    local hashKeyWithField = string.sub(k, 3)
                    local p = string.find(hashKeyWithField, "#")
                    if p then
                        local hashKey = string.sub(hashKeyWithField, 1, p - 1)
                        local field = string.sub(hashKeyWithField, p + 1)
                        redis.call("HDEL", hashKey, field)
                    else
                        redis.call("DEL", hashKeyWithField)
                    end
                else
                    redis.call("DEL", k)
                end
            end
            redis.call("DEL", indexKey)
            return 1
            """;

    private final static String LUA_RESTORE_SCRIPT = """
            local indexKey = KEYS[1]
            for i = 2, #KEYS do
                local k = KEYS[i]
                local v = ARGV[i - 1]
                local prefix = string.sub(k, 1, 2)
            
                if prefix == "L:" then
                    local listKey = string.sub(k, 3)
                    local arr = cjson.decode(v)
                    if arr then
                        redis.call("DEL", listKey)
                        for j = 1, #arr do
                            redis.call("RPUSH", listKey, arr[j])
                        end
                        redis.call("SADD", indexKey, k)
                    end
                elseif prefix == "S:" then
                    local setKeyWithMember = string.sub(k, 3)
                    local p = string.find(setKeyWithMember, "#")
                    if p then
                        local setKey = string.sub(setKeyWithMember, 1, p - 1)
                        local member = string.sub(setKeyWithMember, p + 1)
                        redis.call("SADD", setKey, member)
                        redis.call("SADD", indexKey, k)
                    else
                        local arr = cjson.decode(v)
                        if arr then
                            redis.call("DEL", setKeyWithMember)
                            if #arr > 0 then
                                redis.call("SADD", setKeyWithMember, unpack(arr))
                            end
                            redis.call("SADD", indexKey, k)
                        end
                    end
                elseif prefix == "Z:" then
                    local zsetKeyWithMember = string.sub(k, 3)
                    local p = string.find(zsetKeyWithMember, "#")
                    if p then
                        local zsetKey = string.sub(zsetKeyWithMember, 1, p - 1)
                        local member = string.sub(zsetKeyWithMember, p + 1)
                        redis.call("ZADD", zsetKey, v, member)
                        redis.call("SADD", indexKey, k)
                    else
                        local arr = cjson.decode(v)
                        if arr then
                            redis.call("DEL", zsetKeyWithMember)
                            local n = #arr
                            local j = 1
                            while j < n do
                                redis.call("ZADD", zsetKeyWithMember, arr[j + 1], arr[j])
                                j = j + 2
                            end
                            redis.call("SADD", indexKey, k)
                        end
                    end
                elseif prefix == "H:" then
                    local hashKeyWithField = string.sub(k, 3)
                    local p = string.find(hashKeyWithField, "#")
                    if p then
                        local hashKey = string.sub(hashKeyWithField, 1, p - 1)
                        local field = string.sub(hashKeyWithField, p + 1)
                        redis.call("HSET", hashKey, field, v)
                        redis.call("SADD", indexKey, k)
                    else
                        local arr = cjson.decode(v)
                        if arr then
                            redis.call("DEL", hashKeyWithField)
                            local n = #arr
                            local j = 1
                            while j < n do
                                redis.call("HSET", hashKeyWithField, arr[j], arr[j + 1])
                                j = j + 2
                            end
                            redis.call("SADD", indexKey, k)
                        end
                    end
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

    private Optional<Map<String, String>> dumpByLua(long playerId, Set<String> keys) {
        try {
            if (keys == null || keys.isEmpty()) {
                return Optional.empty();
            }
            DefaultRedisScript<String> script = new DefaultRedisScript<>();
            script.setResultType(String.class);
            script.setScriptText(LUA_SCRIPT);
            String json = redis.execute(script, new ArrayList<>(keys));
            Map<String, String> data = new ObjectMapper().readValue(json, new TypeReference<>() {
            });
            return Optional.of(data);
        } catch (Exception e) {
            log.error("保存玩家数据到mongo失败 playerId:{} ", playerId);
        }
        return Optional.empty();
    }

    public void dumpToMongo(long playerId) {
        String indexKey = "player_keys:" + playerId;
        Set<String> indexedKeys = redis.opsForSet().members(indexKey);
        Optional<Map<String, String>> data = dumpByLua(playerId, indexedKeys);
        if (data.isEmpty()) {
            return;
        }
        Map<String, String> dataMap = data.get();
        if (!dataMap.isEmpty()) {
            PlayerSnapshot snap = new PlayerSnapshot();
            snap.setUid(playerId);
            snap.setKeys(dataMap);
            snap.setUpdateTime(new Date());

            mongo.save(snap);
        }
        deleteByLua(indexedKeys, indexKey);
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

    private void deleteByLua(Set<String> keysToDelete, String indexKey) {
        if (keysToDelete == null || keysToDelete.isEmpty()) return;

        List<String> keys = new ArrayList<>(keysToDelete.size() + 1);
        keys.add(indexKey);
        keys.addAll(keysToDelete);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(LUA_DELETE_SCRIPT);
        script.setResultType(Long.class);
        redis.execute(script, keys);
    }

}
