package com.jjg.game.sim.dao;

import com.alibaba.fastjson.JSON;
import com.jjg.game.sim.constant.CoopTaskConst;
import com.jjg.game.sim.data.CoopRoomRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 协作房间路由记录 DAO (Redis String, JSON)。
 * <p>
 * 记录仅服务路由与预检查, 权威房间态在 slots 节点内存, 由 slots 在成员/状态变更时覆写。
 * TTL 兜底: 节点崩溃后记录自动过期, 任务层据"记录不存在"自愈回退。
 *
 * @author 11
 * @date 2026/7/6
 */
@Repository
public class CoopRoomRecordDao {
    private static final Logger log = LoggerFactory.getLogger(CoopRoomRecordDao.class);
    private static final DefaultRedisScript<Long> ACQUIRE_PLAYER_ROOM = new DefaultRedisScript<>(
            "local current = redis.call('GET', KEYS[1]); " +
                    "if (not current) or current == ARGV[1] then " +
                    "redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2]); return 1; end; return 0;",
            Long.class);
    private static final DefaultRedisScript<Long> RELEASE_PLAYER_ROOM = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]); end; return 0;",
            Long.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void save(CoopRoomRecord record) {
        stringRedisTemplate.opsForValue().set(key(record.getRoomId()), JSON.toJSONString(record),
                Duration.ofSeconds(CoopTaskConst.Redis.ROOM_TTL_SECONDS));
    }

    public CoopRoomRecord get(long roomId) {
        String value = stringRedisTemplate.opsForValue().get(key(roomId));
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(value, CoopRoomRecord.class);
        } catch (Exception e) {
            log.error("解析协作房间记录失败 roomId={},value={}", roomId, value, e);
            return null;
        }
    }

    public void delete(long roomId) {
        stringRedisTemplate.delete(key(roomId));
    }

    /**
     * 全局玩家-房间租约。同一房间重进会刷新 TTL，其他房间不可覆盖。
     */
    public boolean acquirePlayerRoom(long playerId, long roomId) {
        Long result = stringRedisTemplate.execute(ACQUIRE_PLAYER_ROOM,
                Collections.singletonList(playerRoomKey(playerId)), String.valueOf(roomId),
                String.valueOf(CoopTaskConst.Redis.ROOM_TTL_SECONDS));
        return result != null && result == 1L;
    }

    public boolean releasePlayerRoom(long playerId, long roomId) {
        Long result = stringRedisTemplate.execute(RELEASE_PLAYER_ROOM,
                Collections.singletonList(playerRoomKey(playerId)), String.valueOf(roomId));
        return result != null && result == 1L;
    }

    public long getPlayerRoom(long playerId) {
        String value = stringRedisTemplate.opsForValue().get(playerRoomKey(playerId));
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("解析协作玩家房间租约失败 playerId={},value={}", playerId, value, e);
            return 0L;
        }
    }

    /**
     * slots 节点启动恢复使用。SCAN 避免 Redis KEYS 阻塞，并只返回属于指定节点的房间记录。
     */
    public List<CoopRoomRecord> findByNodePath(String nodePath) {
        if (nodePath == null || nodePath.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> keys = stringRedisTemplate.execute((RedisCallback<List<String>>) connection -> {
            List<String> result = new ArrayList<>();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(ScanOptions.scanOptions()
                    .match(CoopTaskConst.Redis.ROOM_KEY_PREFIX + "*").count(200).build())) {
                while (cursor.hasNext()) {
                    result.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return result;
        });
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<CoopRoomRecord> records = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isEmpty()) {
                continue;
            }
            try {
                CoopRoomRecord record = JSON.parseObject(value, CoopRoomRecord.class);
                if (record != null && nodePath.equals(record.getNodePath())) {
                    records.add(record);
                }
            } catch (Exception e) {
                log.error("恢复协作房间记录解析失败 nodePath={},value={}", nodePath, value, e);
            }
        }
        return records;
    }

    private String key(long roomId) {
        return CoopTaskConst.Redis.ROOM_KEY_PREFIX + roomId;
    }

    private String playerRoomKey(long playerId) {
        return CoopTaskConst.Redis.PLAYER_ROOM_KEY_PREFIX + playerId;
    }
}
