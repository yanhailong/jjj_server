package com.jjg.game.core.service;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterMsgSender;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.message.SessionKickout;
import com.jjg.game.common.net.Connect;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2025/5/26 16:56
 */
@Component
public class PlayerSessionService implements TimerListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    //session超时时间
    private static final int SESSION_TIME_OUT_MINUTES = 30;
    private static final int ONLINE_COUNT_MINUTES = 1;

    public static final String SESSION_TABLE_NAME = "playerSession";
    //在线玩家id
    //public static final String ONLINEPLAYERS = "onlinePlayerIds";

    public static final Map<Long, AtomicInteger> onlineMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate redisTemplate;
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
    @Autowired
    protected MarsCurator marsCurator;


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

        redisTemplate.opsForHash().put(SESSION_TABLE_NAME, playerSessionInfo.getPlayerId(), playerSessionInfo);
        //redisTemplate.opsForSet().add(ONLINEPLAYERS, playerSessionInfo.getPlayerId());
        return playerSessionInfo;
    }

    public PFSession getSession(PlayerSessionInfo playerSessionInfo) {
        if (playerSessionInfo == null) {
            log.debug("获取session失败!playerSessionInfo 为空。");
            return null;
        }
        Connect connect;
        try {
            connect = clusterSystem.getClusterByPath(playerSessionInfo.getNodeName()).getConnect();
        } catch (Exception e) {
            e.printStackTrace();
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
        //Set<String> set = redisTemplate.opsForSet().members(ONLINEPLAYERS);
        //List<PlayerSessionInfo> playerSessionInfos = redisTemplate.opsForHash().mulget(SESSION_TABLE_NAME,set);
        //return playerSessionInfos.stream().filter(Objects::nonNull).collect(Collectors.toList());
        return redisTemplate.opsForHash().entries(SESSION_TABLE_NAME);
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

                if (currentTime - lastActiveTime > TimeHelper.ONE_DAY) {
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
                if (lastActiveTime > 0 && currentTime - lastActiveTime > TimeHelper.ONE_DAY) {
                    keys.add(playerSessionInfo.getPlayerId());
                    log.info("移除超时无效session，playerId={}", playerSessionInfo.getPlayerId());
                }
            }
        }
        if (!keys.isEmpty()) {
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
                checkSessionEvent = new TimerEvent<>(this, "PlayerSession", SESSION_TIME_OUT_MINUTES).withTimeUnit(TimeUnit.MINUTES);
                timerCenter.add(checkSessionEvent);
            }

            if (NodeType.GAME.name().equals(nodeManager.nodeConfig.getType())) {
                onlineCountEvent = new TimerEvent<>(this, "OnlineCount", ONLINE_COUNT_MINUTES).withTimeUnit(TimeUnit.MINUTES);
                timerCenter.add(onlineCountEvent);
            }
        }
    }

    public void changeGameType(long playerId, int gameType, int wareId) {
        PlayerSessionInfo info = getInfo(playerId);
        info.setGameType(gameType);
        info.setWareId(wareId);
        save(info);
    }

    public PlayerSessionInfo enterGameServer(PlayerSessionInfo playerSessionInfo, int roomId) {
        onlineCount(playerSessionInfo.getPlayerId());
        playerLastGameInfo(playerSessionInfo.getPlayerId(), 0, playerSessionInfo.getGameType(), playerSessionInfo.getWareId(), roomId);

        return playerSessionInfo;
    }

    /**
     * 添加本节点在线人数
     */
    public void onlineCount(long playerId) {
        AtomicInteger result = onlineMap.computeIfPresent(playerId, (k, v) -> {
            v.incrementAndGet();
            return v;
        });

        if (result != null) {
            return;
        }

        AtomicInteger num = new AtomicInteger(1);
        result = onlineMap.computeIfAbsent(playerId, k -> num);

        if (result == num) {
            return;
        }

        onlineMap.computeIfPresent(playerId, (k, v) -> {
            v.incrementAndGet();
            return v;
        });
    }

    /**
     * 减少本节点在线人数
     */
    public void offlineCount(long playerId) {
        onlineMap.computeIfPresent(playerId, (k, v) -> {
            if (v.get() > 0) {
                v.addAndGet(-1);
            }
            return v;
        });
    }

    public void offline(long playerId, int gameUniqueId, int gameType, int wareId, int roomId) {
        offlineCount(playerId);
        playerLastGameInfo(playerId, gameUniqueId, gameType, wareId, roomId);
    }

    public void playerLastGameInfo(long playerId, int gameUniqueId, int gameType, int wareId, int roomId) {
        PlayerLastGameInfo playerLastGameInfo = new PlayerLastGameInfo();
        playerLastGameInfo.setPlayerId(playerId);
        playerLastGameInfo.setGameUniqueId(gameUniqueId);
        playerLastGameInfo.setGameType(gameType);
        playerLastGameInfo.setWareId(wareId);
        playerLastGameInfo.setRoomId(roomId);
        playerLastGameInfo.setNodePath(nodeManager.getNodePath());
        playerLastGameInfoDao.save(playerLastGameInfo);
    }

    public boolean online(Long playerId) {
        return redisTemplate.opsForHash().hasKey(SESSION_TABLE_NAME, playerId);
    }

    public void changeSessionInfo(PFSession pfSession, Player player) {
        changeSessionInfo(pfSession, player, 0, 0);
    }

    /**
     * 修改session信息
     *
     * @param pfSession
     * @param player
     */
    public void changeSessionInfo(PFSession pfSession, Player player, int gameType, int wareId) {
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
                info.setWareId(wareId);
                log.info("顶号成功! playerId = {}", player.getId());
            }
        } else {
            info = new PlayerSessionInfo();
            info.setNodeName(pfSession.gatePath);
            info.setPlayerId(player.getId());
            info.setGameType(gameType);
            info.setWareId(wareId);
            info.setSessionId(pfSession.sessionId());
        }
        save(info);
    }

    @Override
    public void onTimer(TimerEvent e) {
        if (e == checkSessionEvent && marsCurator.master(NodeType.HALL.getValue())) {
            log.info("开始执行session检查");
            checkSessionByNode();
            Iterator<Map.Entry<String, PFSession>> iterator = clusterSystem.sessionMap().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, PFSession> entry = iterator.next();
                PFSession pfSession = entry.getValue();
                //如果seesion 长时间没有活跃，检查玩家是否还在线
                if (e.getCurrentTime() - pfSession.activeTime > SESSION_TIME_OUT_MINUTES * TimeHelper.ONE_MINUTE) {
                    PlayerSessionInfo ps = getInfo(pfSession.getPlayerId());
                    if (ps == null) {
                        log.warn("移除无效session，playerId={}", pfSession.getPlayerId());
                        iterator.remove();
                        //offlineCount(spiltInfo(ps.getOnlineKey()));
                    } else if (pfSession.getReference() == null) {
                        log.warn("移除无效session，playerId={},sessionId={}", pfSession.getPlayerId(), pfSession.sessionId());
                        iterator.remove();
                        offlineCount(ps.getPlayerId());
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
