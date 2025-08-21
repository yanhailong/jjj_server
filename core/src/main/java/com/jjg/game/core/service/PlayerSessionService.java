package com.jjg.game.core.service;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterMsgSender;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.message.SessionKickout;
import com.jjg.game.common.netty.NettyConnect;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.PlayerLastGameInfoDao;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerLastGameInfo;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.logger.CoreLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/5/26 16:56
 */
@Component
public class PlayerSessionService implements TimerListener<String> {
    public static final String SESSION_TABLE_NAME = "playerSession";
    //session超时时间
    private static final int SESSION_TIME_OUT_MINUTES = 30;
    private static final int ONLINE_COUNT_MINUTES = 1;

    //在线玩家id
    //public static final String ONLINEPLAYERS = "onlinePlayerIds";
    @Autowired
    protected MarsCurator marsCurator;
    private Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private TimerCenter timerCenter;
    @Autowired
    private ClusterMsgSender clusterMsgSender;
    @Autowired
    private CoreLogger coreLogger;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private PlayerLastGameInfoDao playerLastGameInfoDao;
    private TimerEvent<String> checkSessionEvent;
    private TimerEvent<String> onlineCountEvent;

    public PlayerSessionInfo getInfo(long playerId) {
        return (PlayerSessionInfo) redisTemplate.opsForHash().get(SESSION_TABLE_NAME, playerId);
    }

    public boolean hasSession(long playerId) {
        return redisTemplate.opsForHash().hasKey(SESSION_TABLE_NAME, playerId);
    }

    public PlayerSessionInfo save(PlayerSessionInfo playerSessionInfo) {
        if (playerSessionInfo == null) {
            return null;
        }
        //设置活跃时间
        playerSessionInfo.setLastActiveTime(System.currentTimeMillis());
        //设置当前节点
        playerSessionInfo.setCurrentNode(clusterSystem.getNodePath());

        log.debug("设置当前节点 playerId = {},node = {}", playerSessionInfo.getPlayerId(), playerSessionInfo.getCurrentNode());
        redisTemplate.opsForHash().put(SESSION_TABLE_NAME, playerSessionInfo.getPlayerId(), playerSessionInfo);
        //redisTemplate.opsForSet().add(ONLINEPLAYERS, playerSessionInfo.getPlayerId());
        return playerSessionInfo;
    }

    public PFSession getSession(PlayerSessionInfo playerSessionInfo) {
        if (playerSessionInfo == null) {
            log.debug("获取session失败!playerSessionInfo 为空。");
            return null;
        }
        NettyConnect<Object> connect;
        try {
            connect = clusterSystem.getClusterByPath(playerSessionInfo.getNodeName()).getConnect();
        } catch (Exception e) {
            log.error("获取Netty连接失败!playerSessionInfo={},nodeName={}", playerSessionInfo,
                playerSessionInfo.getNodeName(), e);
            return null;
        }
        if (connect == null) {
            log.debug("获取session失败!connect 为空。");
            return null;
        }
        return new PFSession(playerSessionInfo.getSessionId(), connect, null);
    }

    public PFSession getSession(long playerId) {
        return getSession(getInfo(playerId));
    }

    public void sendAll(Object object) {
        clusterMsgSender.broadcast2Gates(object);
    }

    public void remove(long playerId) {
        redisTemplate.opsForHash().delete(SESSION_TABLE_NAME, playerId);
        //redisTemplate.opsForSet().remove(ONLINEPLAYERS, playerId);
    }

    public Map<Long, PlayerSessionInfo> getAll() {
        HashOperations<String, Long, PlayerSessionInfo> hashOperations = redisTemplate.opsForHash();
        return hashOperations.entries(SESSION_TABLE_NAME);
    }


    public void removePlayerSessionByGate(String gatePath) {
        if (gatePath == null || gatePath.isEmpty()) {
            return;
        }
        Map<Long, PlayerSessionInfo> playerSessionInfoMap = getAll();
        List<Long> keys = new ArrayList<>();
        if (playerSessionInfoMap != null) {
            playerSessionInfoMap.entrySet().forEach(en -> {
                if (gatePath.equals(en.getValue().getNodeName())) {
                    keys.add(en.getValue().getPlayerId());
                }
            });
        }
        if (!keys.isEmpty()) {
            redisTemplate.opsForHash().delete(SESSION_TABLE_NAME, keys.toArray());
        }
    }

    public void removeSessionByNode() {
        String currentNode = clusterSystem.getNodePath();
        Map<Long, PlayerSessionInfo> playerSessionInfoMap = getAll();
        List<Long> keys = new ArrayList<>();
        if (playerSessionInfoMap != null) {
            playerSessionInfoMap.entrySet().forEach(en -> {
                if (currentNode.equals(en.getValue().getCurrentNode())) {
                    keys.add(en.getValue().getPlayerId());
                }
            });
        }
        if (!keys.isEmpty()) {
            //log.debug("删除session表的id  keys={}",keys);
            Long num = redisTemplate.opsForHash().delete(SESSION_TABLE_NAME, keys.toArray());
            log.debug("删除session表的结果  num={}", num);
        }
    }

    /**
     * 检查当前节点的session
     */
    public void checkSessionByNode() {
        String currentNode = clusterSystem.getNodePath();
        Map<Long, PlayerSessionInfo> playerSessionInfoMap = getAll();
        List<Long> keys = new ArrayList<>();
        if (playerSessionInfoMap != null) {
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<Long, PlayerSessionInfo> en : playerSessionInfoMap.entrySet()) {
                PlayerSessionInfo playerSessionInfo = en.getValue();
                long lastActiveTime = playerSessionInfo.getLastActiveTime();

                if (currentTime - lastActiveTime > TimeHelper.ONE_DAY_OF_MILLIS) {
                    keys.add(playerSessionInfo.getPlayerId());

                    log.info("清除僵尸 playerId={}", playerSessionInfo.getPlayerId());
                    continue;
                }

                if (!currentNode.equals(playerSessionInfo.getCurrentNode())) {
                    continue;
                }
                if (clusterSystem.sessionMap().containsKey(playerSessionInfo.getSessionId())) {
                    continue;
                }
                //session 的活跃时间超过三个小时的
                if (lastActiveTime > 0 && currentTime - lastActiveTime > TimeHelper.ONE_DAY_OF_MILLIS) {
                    keys.add(playerSessionInfo.getPlayerId());
                    log.info("移除超时无效session，playerId={}", playerSessionInfo.getPlayerId());
                }
            }
        }
        if (!keys.isEmpty()) {
            log.debug("移除玩家： {} 过期session", keys.stream().map(String::valueOf).collect(Collectors.joining(",")));
            redisTemplate.opsForHash().delete(SESSION_TABLE_NAME, keys.toArray());
            //redisTemplate.opsForSet().remove(ONLINEPLAYERS, keys.toArray());
        }
    }

    public void shutdown() {
        removeSessionByNode();
    }

    public void init() {
        if (timerCenter != null) {
            if (NodeType.HALL.name().equals(nodeManager.nodeConfig.getType())) {
                checkSessionEvent =
                    new TimerEvent<>(this, "PlayerSession", SESSION_TIME_OUT_MINUTES).withTimeUnit(TimeUnit.MINUTES);
                timerCenter.add(checkSessionEvent);
            }

            if (NodeType.HALL.name().equals(nodeManager.nodeConfig.getType()) || NodeType.GAME.name().equals(nodeManager.nodeConfig.getType())) {
                onlineCountEvent =
                    new TimerEvent<>(this, "OnlineCount", ONLINE_COUNT_MINUTES).withTimeUnit(TimeUnit.MINUTES);
                timerCenter.add(onlineCountEvent);
            }
        }
    }

    public void changeGameType(long playerId, int gameType, int roomCfgId) {
        PlayerSessionInfo info = getInfo(playerId);
        info.setGameType(gameType);
        info.setRoomCfgId(roomCfgId);
        save(info);
    }

    public PlayerSessionInfo enterGameServer(Player player) {
        return enterGameServer(player,false,null);
    }

    public PlayerSessionInfo enterGameServer(Player player, boolean halfwayOffline, String extra) {
        playerLastGameInfo(player,0,halfwayOffline,extra);
        PlayerSessionInfo info = getInfo(player.getId());
        save(info);
        return info;
    }


    public void offline(Player player, boolean halfwayOffline) {
        offline(player,0,halfwayOffline,null);
    }

    public void offline(Player player, boolean halfwayOffline,String extra) {
        offline(player,0,halfwayOffline,extra);
    }

    public void offline(Player player, int gameUniqueId, boolean halfwayOffline,String extra) {
        playerLastGameInfo(player, gameUniqueId, halfwayOffline, extra);
    }

    public void playerLastGameInfo(Player player, int gameUniqueId, boolean halfwayOffline,String extra) {
        PlayerLastGameInfo playerLastGameInfo = new PlayerLastGameInfo();
        playerLastGameInfo.setPlayerId(player.getId());
        playerLastGameInfo.setGameUniqueId(gameUniqueId);
        playerLastGameInfo.setGameType(player.getGameType());
        playerLastGameInfo.setRoomCfgId(player.getRoomCfgId());
        playerLastGameInfo.setRoomId(player.getRoomId());
        playerLastGameInfo.setHalfwayOffline(halfwayOffline);
        playerLastGameInfo.setExtra(extra);
        playerLastGameInfo.setNodePath(nodeManager.getNodePath());
        playerLastGameInfoDao.save(playerLastGameInfo);
    }

    public boolean online(Long playerId) {
        return redisTemplate.opsForHash().hasKey(SESSION_TABLE_NAME, playerId);
    }

    public void changeSessionInfo(PFSession pfSession, Player player) {
        changeSessionInfo(pfSession, player, player.getGameType(), player.getRoomCfgId());
    }

    /**
     * 修改session信息
     *
     * @param pfSession
     * @param player
     */
    public void changeSessionInfo(PFSession pfSession, Player player, int gameType, int roomCfgId) {
        PlayerSessionInfo info = getInfo(player.getId());
        if (info != null) {
            if (Objects.equals(pfSession.sessionId(), info.getSessionId())) {
                log.debug("session相同，不处理  session={},playerId={}", pfSession.sessionId(), player.getId());
                return;
            }
            //顶号
            ClusterClient clusterClient = clusterSystem.getClusterByPath(info.getNodeName());
            if (clusterClient != null) {
                SessionKickout sessionKickout = new SessionKickout();
                sessionKickout.sessionId = info.getSessionId();
                sessionKickout.playerId = player.getId();
                ClusterMessage clusterMessage = new ClusterMessage(MessageUtil.getPFMessage(sessionKickout));
                try {
                    clusterClient.getConnect().write(clusterMessage);
                } catch (InterruptedException e) {
                    log.error("顶号切换节点失败", e);
                }
                //保存
                info.setPlayerId(player.getId());
                info.setSessionId(pfSession.sessionId());
                info.setNodeName(pfSession.gatePath);
                info.setGameType(gameType);
                info.setRoomCfgId(roomCfgId);
                log.info("顶号成功! playerId = {}", player.getId());
            }
        } else {
            info = new PlayerSessionInfo();
            info.setNodeName(pfSession.gatePath);
            info.setPlayerId(player.getId());
            info.setGameType(gameType);
            info.setRoomCfgId(roomCfgId);
            info.setSessionId(pfSession.sessionId());
        }
        save(info);
    }

    @Override
    public void onTimer(TimerEvent<String> e) {
        if (e == checkSessionEvent && marsCurator.isMaster()) {
            log.info("开始执行session检查");
            checkSessionByNode();
            Iterator<Map.Entry<String, PFSession>> iterator = clusterSystem.sessionMap().entrySet().iterator();
            // 分批处理玩家，如果在线玩家过多，会出现问题
            while (iterator.hasNext()) {
                Map.Entry<String, PFSession> entry = iterator.next();
                PFSession pfSession = entry.getValue();
                //如果session 长时间没有活跃，检查玩家是否还在线
                if (e.getCurrentTime() - pfSession.activeTime > SESSION_TIME_OUT_MINUTES * TimeHelper.ONE_MINUTE_OF_MILLIS) {
                    // TODO 在循环中调用数据库接口？
                    PlayerSessionInfo ps = getInfo(pfSession.getPlayerId());
                    if (ps == null) {
                        log.warn("移除无效session，playerId={}", pfSession.getPlayerId());
                        iterator.remove();
                        //offlineCount(spiltInfo(ps.getOnlineKey()));
                    } else if (pfSession.getReference() == null) {
                        log.warn("移除无效session，playerId={},sessionId={}", pfSession.getPlayerId(),
                            pfSession.sessionId());
                        iterator.remove();
                    }
                }
            }
        } else if (e == onlineCountEvent) {
            int size = clusterSystem.clusterSessionSize();
            log.info("打印在线人数 ,size={}", size);
            coreLogger.online(size, nodeManager.nodeConfig.getTcpAddress().getHost());

        }
    }
}
