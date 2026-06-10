package com.jjg.game.social.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jjg.game.social.dao.FriendDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class SocialRelationCache {
    private static final Logger log = LoggerFactory.getLogger(SocialRelationCache.class);

    //redis发布频道
    public static final String BLACKLIST_INVALIDATE_CHANNEL = "social:blacklist:invalidate";
    //缓存过期时间(秒)
    private static final int BLACKLIST_OWNER_CACHE_SECONDS = 1800;
    //缓存最大数量
    private static final int BLACKLIST_OWNER_CACHE_MAX = 2000;

    //好友id集合 Redis key 前缀 (全节点共享, hall/slots 的状态广播都会读)
    private static final String FRIEND_IDS_KEY_PREFIX = "social:friendids:";
    //好友id集合缓存有效期(秒): 兜底"回源回填与并发失效竞态"造成的脏数据上限, 也让不活跃玩家的 key 自然过期
    private static final long FRIEND_IDS_TTL_SECONDS = TimeUnit.DAYS.toSeconds(1);
    //空集合哨兵成员: 区分"无好友(已缓存)"与"key不存在(未缓存)", 避免无好友玩家每次广播都回源 Mongo
    private static final String FRIEND_IDS_EMPTY_SENTINEL = "0";

    @Autowired
    private FriendDao friendDao;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    //黑名单缓存
    private final Cache<Long, Set<Long>> blacklistCache = Caffeine.newBuilder()
            .expireAfterWrite(BLACKLIST_OWNER_CACHE_SECONDS, TimeUnit.SECONDS)
            .maximumSize(BLACKLIST_OWNER_CACHE_MAX)
            .build();

    /**
     * 检查玩家是否在黑名单中
     *
     * @param ownerId
     * @param targetId
     * @return
     */
    public boolean isBlacklisted(long ownerId, long targetId) {
        if (ownerId <= 0 || targetId <= 0) {
            return false;
        }
        Set<Long> blacklistIds = blacklistCache.get(ownerId, this::loadBlacklistIds);
        return blacklistIds != null && blacklistIds.contains(targetId);
    }

    /**
     * 让本地缓存失效
     *
     * @param ownerId
     */
    public void invalidateLocal(long ownerId) {
        if (ownerId > 0) {
            blacklistCache.invalidate(ownerId);
        }
    }

    /**
     * 加载黑名单列表
     *
     * @param ownerId
     * @return
     */
    private Set<Long> loadBlacklistIds(long ownerId) {
        Set<Long> ids = friendDao.getBlacklistIds(ownerId);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        return ids;
    }

    // ----------------------- 好友id集合 (Redis 共享缓存) -----------------------

    /**
     * 取玩家的好友 id 集合: 优先读 Redis Set (全节点共享), 未命中回源 Mongo(投影) 并回填。
     * <p>
     * 供登录/登出/进游戏的状态广播等高频生命周期路径使用, 把对 Mongo 的全文档读
     * 收敛为一次 Redis SMEMBERS; 好友关系变更时由 {@link #invalidateFriendIds} 删 key 重建。
     */
    public Set<Long> getFriendIds(long ownerId) {
        String key = FRIEND_IDS_KEY_PREFIX + ownerId;
        try {
            Set<String> members = stringRedisTemplate.opsForSet().members(key);
            if (members != null && !members.isEmpty()) {
                Set<Long> ids = new HashSet<>(members.size());
                for (String m : members) {
                    long id = Long.parseLong(m);
                    if (id > 0) {
                        ids.add(id);
                    }
                }
                return ids;
            }
        } catch (Exception e) {
            log.warn("读取好友id缓存失败, 回源DB ownerId={}", ownerId, e);
        }
        Set<Long> ids = friendDao.getFriendIds(ownerId);
        backfillFriendIds(key, ids);
        return ids;
    }

    private void backfillFriendIds(String key, Set<Long> ids) {
        try {
            String[] members = new String[ids.size() + 1];
            int i = 0;
            members[i++] = FRIEND_IDS_EMPTY_SENTINEL;
            for (Long id : ids) {
                members[i++] = String.valueOf(id);
            }
            stringRedisTemplate.opsForSet().add(key, members);
            stringRedisTemplate.expire(key, FRIEND_IDS_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("回填好友id缓存失败 key={}", key, e);
        }
    }

    /**
     * 好友关系变更(同意申请/删除好友)后失效双方的好友id缓存, 下次读取时回源重建。
     * 用删 key 而非增量 SADD/SREM, 规避"key 不存在时增量写出残缺集合"的问题。
     */
    public void invalidateFriendIds(long selfId, Collection<Long> otherIds) {
        List<String> keys = new ArrayList<>();
        keys.add(FRIEND_IDS_KEY_PREFIX + selfId);
        if (otherIds != null) {
            for (Long id : otherIds) {
                if (id != null) {
                    keys.add(FRIEND_IDS_KEY_PREFIX + id);
                }
            }
        }
        try {
            stringRedisTemplate.delete(keys);
        } catch (Exception e) {
            log.warn("失效好友id缓存失败 selfId={},otherIds={}", selfId, otherIds, e);
        }
    }

    /**
     * 发布失效
     *
     * @param ownerId
     */
    public void publishInvalidate(long ownerId) {
        invalidateLocal(ownerId);
        try {
            stringRedisTemplate.convertAndSend(BLACKLIST_INVALIDATE_CHANNEL, String.valueOf(ownerId));
        } catch (Exception e) {
            log.warn("发布黑名单缓存失效消息失败 ownerId={}", ownerId, e);
        }
    }

}
