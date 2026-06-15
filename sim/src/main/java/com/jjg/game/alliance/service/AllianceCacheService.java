package com.jjg.game.alliance.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.dao.AllianceDao;
import com.jjg.game.alliance.dao.AlliancePlayerDao;
import com.jjg.game.alliance.data.AllianceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 联盟读缓存 (全系统读联盟数据的统一入口)。
 * <p>
 * 两级结构, 范式对齐 {@code SocialRelationCache}:
 * <ul>
 *   <li>联盟文档: Caffeine 短 TTL 本地缓存 + Redis pub/sub 失效广播。写操作后调用
 *       {@link #publishInvalidate} 让全节点失效; 即便广播丢失, TTL 也保证脏读上界。</li>
 *   <li>玩家 -> allianceId 映射: 本地 Caffeine + Redis string 共享缓存, 未命中回源
 *       玩家文档投影。该映射是最高频查询 (聊天频道/事件上报/积分归属), 必须挡在 Mongo 之前。</li>
 * </ul>
 * 写路径永远直达 Mongo 原子更新, 缓存只服务读 —— 因此不存在缓存为权威的丢失问题。
 *
 * @author 11
 * @date 2026/6/11
 */
@Component
public class AllianceCacheService {
    private static final Logger log = LoggerFactory.getLogger(AllianceCacheService.class);

    //联盟文档本地缓存 TTL(秒): 失效广播兜底窗口
    private static final int ALLIANCE_CACHE_SECONDS = 30;
    private static final int ALLIANCE_CACHE_MAX = 2000;
    //玩家映射本地缓存 TTL(秒)
    private static final int PLAYER_CACHE_SECONDS = 60;
    private static final int PLAYER_CACHE_MAX = 20000;
    //玩家映射 Redis TTL(秒): 不活跃玩家自然过期
    private static final long PLAYER_REDIS_TTL_SECONDS = TimeUnit.DAYS.toSeconds(1);

    @Autowired
    private AllianceDao allianceDao;
    @Autowired
    private AlliancePlayerDao alliancePlayerDao;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //联盟文档缓存 (负缓存用 NULL_ALLIANCE 占位, 防不存在的联盟被反复回源)
    private final Cache<Long, AllianceData> allianceCache = Caffeine.newBuilder()
            .expireAfterWrite(ALLIANCE_CACHE_SECONDS, TimeUnit.SECONDS)
            .maximumSize(ALLIANCE_CACHE_MAX)
            .build();
    private static final AllianceData NULL_ALLIANCE = new AllianceData();

    //playerId -> allianceId (0=无盟, 也缓存, 避免无盟玩家每次回源)
    private final Cache<Long, Long> playerAllianceCache = Caffeine.newBuilder()
            .expireAfterWrite(PLAYER_CACHE_SECONDS, TimeUnit.SECONDS)
            .maximumSize(PLAYER_CACHE_MAX)
            .build();

    // ----------------------- 联盟文档 -----------------------

    /**
     * 取联盟数据 (本地缓存, 30s 内可能有少量滞后; 强一致场景直接走 DAO)。
     *
     * @return 不存在返回 null
     */
    public AllianceData getAlliance(long allianceId) {
        if (allianceId <= 0) {
            return null;
        }
        AllianceData data = allianceCache.get(allianceId,
                aid -> allianceDao.findById(aid).orElse(NULL_ALLIANCE));
        return data == NULL_ALLIANCE ? null : data;
    }

    /**
     * 写操作后失效联盟缓存: 本地立即失效 + 广播其他节点。
     */
    public void publishInvalidate(long allianceId) {
        invalidateLocal(allianceId);
        try {
            stringRedisTemplate.convertAndSend(AllianceConst.RedisKey.INVALIDATE_CHANNEL, String.valueOf(allianceId));
        } catch (Exception e) {
            log.warn("发布联盟缓存失效消息失败 allianceId={}", allianceId, e);
        }
    }

    /**
     * 本地失效 (Redis 订阅回调也走这里)。
     */
    public void invalidateLocal(long allianceId) {
        if (allianceId > 0) {
            allianceCache.invalidate(allianceId);
        }
    }

    // ----------------------- 玩家 -> 联盟 映射 -----------------------

    /**
     * 玩家所在联盟 id (0=无盟)。
     * 命中顺序: 本地 Caffeine -> Redis -> Mongo 投影 (并回填)。
     */
    public long getAllianceId(long playerId) {
        if (playerId <= 0) {
            return 0;
        }
        Long cached = playerAllianceCache.get(playerId, this::loadAllianceId);
        return cached == null ? 0 : cached;
    }

    private long loadAllianceId(long playerId) {
        String key = AllianceConst.RedisKey.PLAYER_ALLIANCE_PREFIX + playerId;
        try {
            String val = stringRedisTemplate.opsForValue().get(key);
            if (val != null) {
                return Long.parseLong(val);
            }
        } catch (Exception e) {
            log.warn("读取玩家联盟映射缓存失败, 回源DB playerId={}", playerId, e);
        }
        long allianceId = alliancePlayerDao.getAllianceId(playerId);
        try {
            stringRedisTemplate.opsForValue().set(key, String.valueOf(allianceId),
                    PLAYER_REDIS_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("回填玩家联盟映射缓存失败 playerId={}", playerId, e);
        }
        return allianceId;
    }

    /**
     * 玩家联盟关系变更 (加入/退出/被踢/解散) 后失效映射: 删 Redis key + 本地, 下次读取回源重建。
     */
    public void invalidatePlayer(long playerId) {
        playerAllianceCache.invalidate(playerId);
        try {
            stringRedisTemplate.delete(AllianceConst.RedisKey.PLAYER_ALLIANCE_PREFIX + playerId);
        } catch (Exception e) {
            log.warn("失效玩家联盟映射缓存失败 playerId={}", playerId, e);
        }
    }

    /**
     * 批量失效玩家映射 (解散联盟时全员失效)。
     */
    public void invalidatePlayers(java.util.Collection<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return;
        }
        java.util.List<String> keys = new java.util.ArrayList<>(playerIds.size());
        for (Long pid : playerIds) {
            if (pid != null) {
                playerAllianceCache.invalidate(pid);
                keys.add(AllianceConst.RedisKey.PLAYER_ALLIANCE_PREFIX + pid);
            }
        }
        try {
            stringRedisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("批量失效玩家联盟映射缓存失败 size={}", keys.size(), e);
        }
    }
}
