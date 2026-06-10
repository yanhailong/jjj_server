package com.jjg.game.social.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jjg.game.common.cluster.ClusterMsgSender;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.service.PlayerSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 社交消息投递。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class SocialSender {
    private static final Logger log = LoggerFactory.getLogger(SocialSender.class);

    /**
     * 远端玩家会话信息(节点+sessionId)的本地缓存有效期(秒)。
     * 用于避免跨节点点对点高频通信(如私聊)时每条消息都回源 Redis 查询 {@link PlayerSessionInfo}。
     * 取值越大回源越少, 但玩家顶号/重连/切换网关后的过期窗口越长(期间消息可能投递到旧会话被丢弃),
     */
    private static final int SESSION_INFO_CACHE_SECONDS = 30;
    /**
     * 会话信息本地缓存最大条目数, 防止内存无界增长。
     */
    private static final int SESSION_INFO_CACHE_MAX = 2000;

    @Autowired
    private ClusterMsgSender clusterMsgSender;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private ClusterSystem clusterSystem;

    /**
     * 远端玩家会话信息本地缓存 playerId -> PlayerSessionInfo。
     * 仅缓存命中(在线)的记录; 离线(null)不缓存, 避免延迟新上线玩家的消息投递。
     */
    private final Cache<Long, PlayerSessionInfo> sessionInfoCache = Caffeine.newBuilder()
            .expireAfterWrite(SESSION_INFO_CACHE_SECONDS, TimeUnit.SECONDS)
            .maximumSize(SESSION_INFO_CACHE_MAX)
            .build();

    /**
     * 广播给全服在线玩家。
     */
    public void broadcastAll(Object msg) {
        clusterMsgSender.broadcast2Gates(msg);
    }

    /**
     * 发送给指定玩家 (跨节点); 玩家离线则静默跳过。
     *
     * @return 是否成功投递(在线且发送成功)
     */
    public boolean sendTo(long playerId, Object msg) {
        //本节点在线: 直接发
        PFSession local = clusterSystem.getSession(playerId);
        if (local != null) {
            return doSend(local, msg, playerId);
        }
        //跨节点: 优先读本地缓存, 未命中再回源 Redis; 离线为常态, 静默跳过(不查 null 不打 warning)
        PlayerSessionInfo info = getInfoCached(playerId);
        if (info == null) {
            return false;
        }
        return sendByInfo(info, msg, playerId);
    }

    /**
     * 发送给一批玩家。
     */
    public void sendTo(Collection<Long> playerIds, Object msg) {
        if (playerIds == null || playerIds.isEmpty()) {
            return;
        }
        //缓存/本节点未覆盖的玩家集中起来, 一次 multiGet 批量回源, 避免逐个 HGET
        List<Long> needFetch = null;
        for (Long id : playerIds) {
            if (id == null) {
                continue;
            }
            PFSession local = clusterSystem.getSession(id);
            if (local != null) {
                doSend(local, msg, id);
                continue;
            }
            PlayerSessionInfo cached = sessionInfoCache.getIfPresent(id);
            if (cached != null) {
                sendByInfo(cached, msg, id);
                continue;
            }
            if (needFetch == null) {
                needFetch = new ArrayList<>();
            }
            needFetch.add(id);
        }
        if (needFetch == null) {
            return;
        }
        List<PlayerSessionInfo> infos = playerSessionService.getInfos(needFetch);
        if (infos == null) {
            return;
        }
        for (int i = 0; i < needFetch.size(); i++) {
            PlayerSessionInfo info = infos.get(i);
            if (info == null) {
                continue;
            }
            long id = needFetch.get(i);
            sessionInfoCache.put(id, info);
            sendByInfo(info, msg, id);
        }
    }

    /**
     * 玩家是否在线 (全服)。
     */
    public boolean online(long playerId) {
        return playerSessionService.online(playerId);
    }

    /**
     * 读取远端玩家会话信息: 命中本地缓存直接返回, 未命中回源 Redis 并缓存(仅缓存在线记录)。
     */
    private PlayerSessionInfo getInfoCached(long playerId) {
        PlayerSessionInfo info = sessionInfoCache.getIfPresent(playerId);
        if (info != null) {
            return info;
        }
        info = playerSessionService.getInfo(playerId);
        if (info != null) {
            sessionInfoCache.put(playerId, info);
        }
        return info;
    }

    /**
     * 按会话信息构造 PFSession 并投递; 投递失败说明会话可能已失效, 剔除缓存让下次回源。
     */
    private boolean sendByInfo(PlayerSessionInfo info, Object msg, long playerId) {
        PFSession session = playerSessionService.getSession(info);
        if (session == null) {
            sessionInfoCache.invalidate(playerId);
            return false;
        }
        boolean ok = doSend(session, msg, playerId);
        if (!ok) {
            sessionInfoCache.invalidate(playerId);
        }
        return ok;
    }

    private boolean doSend(PFSession session, Object msg, long playerId) {
        try {
            session.send(msg);
            return true;
        } catch (Exception e) {
            log.warn("社交点对点投递失败 playerId={}", playerId, e);
            return false;
        }
    }
}
