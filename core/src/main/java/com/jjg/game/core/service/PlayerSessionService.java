package com.jjg.game.core.service;

import com.alibaba.fastjson.JSONArray;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterMsgSender;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.listener.SessionLogoutListener;
import com.jjg.game.common.message.SessionKickout;
import com.jjg.game.common.netty.NettyConnect;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.OnlinePlayerDao;
import com.jjg.game.core.dao.PlayerLastGameInfoDao;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerLastGameInfo;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.logger.CoreLogger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/5/26 16:56
 */
@Component
public class PlayerSessionService implements TimerListener<String>, SessionLogoutListener {
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
    private StringRedisTemplate stringRedisTemplate;
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
    private OnlinePlayerDao onlinePlayerDao;
    @Autowired
    private AccountDao accountDao;

    // Lua 脚本：获取并删除
    private final String luaScript = """
            local value = redis.call('HGET', KEYS[1], ARGV[1])
            if value then
                redis.call('HDEL', KEYS[1], ARGV[1])
            end
            return value
            """;

    private TimerEvent<String> onlineCountEvent;

    public PlayerSessionInfo getInfo(long playerId) {
        return (PlayerSessionInfo) redisTemplate.opsForHash().get(SESSION_TABLE_NAME, playerId);
    }

    public List<PlayerSessionInfo> getInfos(List<Long> playerId) {
        HashOperations<String, Long, PlayerSessionInfo> opsForHash = redisTemplate.opsForHash();
        return opsForHash.multiGet(SESSION_TABLE_NAME, playerId);
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

        log.debug("设置当前节点 playerId = {},node = {}",
                playerSessionInfo.getPlayerId(), playerSessionInfo.getCurrentNode());
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
            log.error("获取Netty连接失败!playerSessionInfo={},nodeName={}",
                    playerSessionInfo, playerSessionInfo.getNodeName(), e);
            return null;
        }
        if (connect == null) {
            log.debug("获取session失败!connect 为空。");
            return null;
        }

        PFSession pfSession = new PFSession(playerSessionInfo.getSessionId(), connect, null);
        pfSession.setPlayerId(playerSessionInfo.getPlayerId());
        return pfSession;
    }

    /**
     * 根据玩家id获取session
     *
     * @param playerId
     * @return
     */
    public PFSession getSession(long playerId) {
        PFSession session = clusterSystem.getSession(playerId);
        if (session == null) {
            session = getSession(getInfo(playerId));
        }
        return session;
    }

    public void sendAll(Object object) {
        clusterMsgSender.broadcast2Gates(object);
    }

    public PlayerSessionInfo remove(long playerId) {
        onlinePlayerDao.delete(playerId);

        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptText(luaScript);
        script.setResultType(String.class);

        String result = stringRedisTemplate.execute(
                script,
                Collections.singletonList(SESSION_TABLE_NAME),
                String.valueOf(playerId)
        );
        if (StringUtils.isNotEmpty(result)) {
            JSONArray array = JSONArray.parseArray(result);
            if(array != null && array.size() > 1) {
                return array.getObject(1, PlayerSessionInfo.class);
            }
        }

        return null;
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
            onlinePlayerDao.delete(keys);
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
            onlinePlayerDao.delete(keys);
            log.debug("删除session表的结果  num={}", num);
        }
    }

    /**
     * 检查当前节点的session
     */
    public List<Long> checkSessionByNode() {
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
                if (clusterSystem.existSession(playerSessionInfo.getSessionId())) {
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
            onlinePlayerDao.delete(keys);
            //redisTemplate.opsForSet().remove(ONLINEPLAYERS, keys.toArray());
        }
        return keys;
    }

    public void shutdown() {
        removeSessionByNode();
    }

    public void init() {
        if (timerCenter != null) {

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
        onlinePlayerDao.changeGameType(playerId, gameType, roomCfgId);
    }

    public PlayerSessionInfo enterGameServer(Player player) {
        return enterGameServer(player, false, null);
    }

    public PlayerSessionInfo enterGameServer(Player player, boolean halfwayOffline, String extra) {
        playerLastGameInfo(player, 0, halfwayOffline, extra);
        PlayerSessionInfo info = getInfo(player.getId());
        save(info);
        onlinePlayerDao.changeGameType(player.getId(), player.getGameType(), player.getRoomCfgId());
        return info;
    }


    public void offline(Player player, boolean halfwayOffline) {
        offline(player, 0, halfwayOffline, null);
    }

    public void offline(Player player, boolean halfwayOffline, String extra) {
        offline(player, 0, halfwayOffline, extra);
    }

    public void offline(Player player, int gameUniqueId, boolean halfwayOffline, String extra) {
        playerLastGameInfo(player, gameUniqueId, halfwayOffline, extra);
    }

    public void playerLastGameInfo(Player player, int gameUniqueId, boolean halfwayOffline, String extra) {
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

    /**
     * 更新玩家当前所在节点
     *
     * @param pfSession
     * @param player
     */
    public void updateNodePath(PFSession pfSession, Player player) {
        PlayerSessionInfo info = getInfo(player.getId());
        if (info == null) {
            info = new PlayerSessionInfo();
            info.setNodeName(pfSession.gatePath);
            info.setPlayerId(player.getId());
            info.setGameType(player.getGameType());
            info.setRoomCfgId(player.getRoomCfgId());
            info.setSessionId(pfSession.sessionId());
            info.setCreateTime(TimeHelper.nowInt());
        } else {
            info.setGameType(player.getGameType());
            info.setRoomCfgId(player.getRoomCfgId());
            info.setSessionId(pfSession.sessionId());
        }
        save(info);
        onlinePlayerDao.changeGameType(player.getId(), player.getGameType(), player.getRoomCfgId());
    }

    /**
     * 更新断线重连状态
     */
    public void updateReconnectStatus(boolean reconnectStatus, PlayerSessionInfo playerSessionInfo) {
        if (reconnectStatus != playerSessionInfo.isReconnect()) {
            playerSessionInfo.setReconnect(reconnectStatus);
            save(playerSessionInfo);
        }
    }

    public PlayerSessionInfo online(PFSession pfSession, Player player) {
        return online(pfSession, player, player.getGameType(), player.getRoomCfgId());
    }

    /**
     * 修改session信息
     *
     * @param pfSession
     * @param player
     */
    public PlayerSessionInfo online(PFSession pfSession, Player player, int gameType, int roomCfgId) {
        PlayerSessionInfo info = getInfo(player.getId());
        if (info != null) {
            if (Objects.equals(pfSession.sessionId(), info.getSessionId())) {
                log.debug("session相同，不处理  session={},playerId={}", pfSession.sessionId(), player.getId());
                return info;
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
            info.setCreateTime(TimeHelper.nowInt());
        }
        save(info);
        onlinePlayerDao.online(player.getId(), player.getChannel().getValue(), gameType, player.getRoomCfgId(), player.getSubChannel());
        return info;
    }

    @Override
    public void onTimer(TimerEvent<String> e) {
        if (e == onlineCountEvent) {
            int size = clusterSystem.clusterSessionSize();
            log.info("打印在线人数 ,size={}", size);
            coreLogger.online(size, nodeManager.nodeConfig.getTcpAddress().getHost());

        }
    }


    @Override
    public void logout(long playerId, String sessionId) {
        accountDao.checkAndSave(playerId, a -> a.setLastOfflineTime(System.currentTimeMillis()));
    }
}
