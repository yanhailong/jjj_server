package com.jjg.game.sim.dao;

import com.alibaba.fastjson.JSON;
import com.jjg.game.sim.constant.CoopTaskConst;
import com.jjg.game.sim.data.CoopRoomRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

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

    /**
     * 仅判存在性 (自愈检查用, 避免把整个 JSON 读回并反序列化)。
     */
    public boolean exists(long roomId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key(roomId)));
    }

    public void delete(long roomId) {
        stringRedisTemplate.delete(key(roomId));
    }

    private String key(long roomId) {
        return CoopTaskConst.Redis.ROOM_KEY_PREFIX + roomId;
    }
}
