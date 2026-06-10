package com.jjg.game.social.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.social.pb.res.NotifyFriendStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 好友在线状态: 查询 + 变更广播。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class SocialStatusService {
    private static final Logger log = LoggerFactory.getLogger(SocialStatusService.class);

    public static final int OFFLINE = 0;
    public static final int ONLINE = 1;
    public static final int IN_GAME = 2;

    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private SocialSender sender;
    @Autowired
    private SocialRelationCache relationCache;

    /**
     * 玩家状态: 0离线 1在线 2游戏中。
     */
    public int statusOf(long playerId) {
        return statusOf(playerSessionService.getInfo(playerId));
    }

    /**
     * 由会话信息推导状态, 供已批量取到 {@link PlayerSessionInfo} 的场景复用, 避免重复回源。
     */
    public int statusOf(PlayerSessionInfo info) {
        if (info == null) {
            return OFFLINE;
        }
        //在游戏场次中视为"游戏中" (best-effort; 大厅 roomCfgId 为 0)
        return info.getRoomCfgId() > 0 ? IN_GAME : ONLINE;
    }

    /**
     * 离线时长(秒); 在线返回 0。
     * 注: 暂以玩家数据更新时间近似最近活跃, 精确"最后下线时间"待后续在登出时持久化。
     */
    public long offlineSeconds(long playerId, Player player) {
        return offlineSeconds(playerSessionService.getInfo(playerId), player);
    }

    /**
     * 由会话信息推导离线时长, 供已批量取到 {@link PlayerSessionInfo} 的场景复用 (info 非空即在线, 返回 0)。
     */
    public long offlineSeconds(PlayerSessionInfo info, Player player) {
        if (info != null) {
            return 0;
        }
        if (player != null && player.getUpdateTime() > 0) {
            long sec = (System.currentTimeMillis() - player.getUpdateTime()) / 1000;
            return Math.max(sec, 0);
        }
        return 0;
    }

    /**
     * 批量取一批玩家的会话信息 (一次 Redis HMGET); 离线玩家不入结果。
     * 用于好友列表/会话列表等需要批量判定在线状态的场景, 替代逐个 getInfo/online 的 N+1。
     *
     * @return playerId -> 会话信息 (仅含在线者)
     */
    public Map<Long, PlayerSessionInfo> infosOf(Collection<Long> playerIds) {
        Map<Long, PlayerSessionInfo> map = new HashMap<>();
        if (playerIds == null || playerIds.isEmpty()) {
            return map;
        }
        List<Long> ids = new ArrayList<>(playerIds);
        List<PlayerSessionInfo> infos = playerSessionService.getInfos(ids);
        if (infos == null) {
            return map;
        }
        for (int i = 0; i < ids.size() && i < infos.size(); i++) {
            PlayerSessionInfo info = infos.get(i);
            if (info != null) {
                map.put(ids.get(i), info);
            }
        }
        return map;
    }

    /**
     * 向"我"的在线好友广播我的最新状态 (登录/登出/切场景触发)。
     *
     * @param playerId
     */
    public void broadcastStatus(long playerId, int status) {
        try {
            //好友id走 Redis 共享缓存: 登录/登出/进游戏是高频生命周期事件, 不读 Mongo
            Set<Long> friendIds = relationCache.getFriendIds(playerId);
            if (friendIds.isEmpty()) {
                return;
            }

            NotifyFriendStatus notify = new NotifyFriendStatus(Code.SUCCESS);
            notify.playerId = playerId;
            notify.status = status;

            //批量推送: 未命中本地缓存的好友会话信息一次 HMGET 回源, 避免逐个 getInfo
            sender.sendTo(friendIds, notify);
        } catch (Exception e) {
            log.error("广播好友状态失败 playerId={}", playerId, e);
        }
    }
}
