package com.jjg.game.common.cluster;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.*;
import com.jjg.game.common.gate.GateClusterMessageDispatcher;
import com.jjg.game.common.message.SwitchNodeMessage;
import com.jjg.game.common.net.NetAddress;
import com.jjg.game.common.netty.ConnectPool;
import com.jjg.game.common.netty.NettyConnect;
import com.jjg.game.common.netty.NettyServer;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.TimeHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * 集群系统总线
 *
 * @author nobody
 * @since 1.0
 */
@Component
@Order(1)
//public class ClusterSystem implements MarsNodeListener, ApplicationListener<ContextRefreshedEvent>,
// CommandLineRunner, TimerListener {
public class ClusterSystem implements MarsNodeListener, TimerListener<String> {

    private static final AtomicBoolean created = new AtomicBoolean(false);
    public static ClusterSystem system;

    //session超时时间
    private static final int SESSION_TIME_OUT_MINUTES = 30;

    /**
     * 集群各个节点客户端对象
     */
    private final Map<MarsNode, ClusterClient> clusterClientMap = new ConcurrentHashMap<>();

    /**
     * 玩家session集合 sessionId <==> session对象
     */
    private final Map<String, PFSession> sessionMap = new ConcurrentHashMap<>();
    /**
     * 玩家id到sessionId的集合 playerId <==> sessionId
     */
    private final Map<Long, String> playerIdSessionMap = new ConcurrentHashMap<>();
    @Autowired
    public NodeManager nodeManager;
    @Autowired
    public MarsCurator marsCurator;
    @Autowired
    public NodeConfig nodeConfig;
    public TimerCenter timerCenter;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private ClusterMessageDispatcher clusterMessageDispatcher;
    /**
     * 连接通道初始化器
     */
    private ClusterConnectInitializer initializer;
    private TimerEvent<String> clusterSystemEvent;

    /**
     * 等待移除的连接Id
     */
    private final ConcurrentHashSet<String> deleteSet = new ConcurrentHashSet<>();

    /**
     * 定时删除session定时器
     */
    private TimerEvent<String> deleteTimerEvent;

    @Bean
    public ClusterMessageDispatcher clusterMessageDispatcher() {
        if (NodeType.GATE.toString().equals(nodeManager.nodeConfig.getType())) {
            return clusterMessageDispatcher = new GateClusterMessageDispatcher(this);
        } else {
            return clusterMessageDispatcher = new ClusterMessageDispatcher(this);
        }
    }

    @Bean
    public ClusterConnectInitializer createClusterConnectInitializer() {
        initializer = new ClusterConnectInitializer(clusterMessageDispatcher);
        return initializer;
    }

    /**
     * 广播消息给所有在线玩家
     *
     * @param notify 通知消息
     * @param <T>    t
     */
    public <T> void broadcastToOnlinePlayer(T notify) {
        broadcastToOnlinePlayer(notify, CoreConst.Session.SESSION_BROADCAST_BATCH_LIMIT);
    }

    /**
     * 发送消息给玩家
     *
     * @param notify   通知消息
     * @param playerId 玩家id
     * @param <T>      消息
     */
    public <T> void sendToPlayer(T notify, long playerId) {
        String sessionStr = playerIdSessionMap.get(playerId);
        if (StringUtils.isNotEmpty(sessionStr)) {
            PFSession session = sessionMap.get(sessionStr);
            if (session.getConnect() != null && session.getConnect().isActive()) {
                session.send(notify);
            }
        }
    }

    /**
     * 广播消息给所有在线玩家
     *
     * @param notify     通知消息
     * @param batchBlock 分批发送，每批次发送多少
     * @param <T>        t
     */
    public <T> void broadcastToOnlinePlayer(T notify, int batchBlock) {
        // 如果在线人数超过指定分批发送限制值
        if (sessionMap.size() > batchBlock) {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<PFSession> sessionList = new ArrayList<>(sessionMap.values());
                int sessionSize = sessionList.size(), batchSize =
                        (int) Math.ceil(sessionList.size() / (batchBlock * 1.0));
                for (int i = 0; i < batchSize; i++) {
                    int startP = i * batchBlock;
                    int endP = Math.min((i + 1) * batchBlock, sessionSize);
                    List<PFSession> subList = sessionList.subList(startP, endP);
                    // 分批次向客户端发送，避免同一时间发送大量消息
                    executor.submit(() -> {
                        if (!subList.isEmpty()) {
                            subList.forEach(pf -> {
                                if (pf.getConnect() != null && pf.getConnect().isActive()) {
                                    pf.send(notify);
                                }
                            });
                        }
                    });
                }
            }
        } else {
            sessionMap.forEach((key, session) -> {
                if (session.getConnect() != null && session.getConnect().isActive()) {
                    session.send(notify);
                }
            });
        }
    }

    /**
     * 是否存在session
     */
    public boolean existSession(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            return false;
        }
        return sessionMap.containsKey(sessionId);
    }

    /**
     * 通过sessionId获取session
     */
    public PFSession getSession(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            return null;
        }
        return sessionMap.get(sessionId);
    }

    /**
     * 通过玩家id获取session
     */
    public PFSession getSession(long playerId) {
        if (playerId <= 0 || CollectionUtil.isEmpty(playerIdSessionMap)) {
            return null;
        }
        String sessionId = playerIdSessionMap.get(playerId);
        if (sessionId == null) {
            return null;
        }
        return sessionMap.get(sessionId);
    }

    /**
     * 获取节点所有玩家的玩家id
     *
     * @return
     */
    public List<Long> getAllPlayerIds() {
        return new ArrayList<>(playerIdSessionMap.keySet());
    }



    /**
     * 添加session
     */
    public void putSession(String sessionId, PFSession pfSession) {
        if (!StringUtils.isEmpty(sessionId) && pfSession != null) {
            sessionMap.put(sessionId, pfSession);
            playerIdSessionMap.put(pfSession.playerId, sessionId);
        }
    }

    /**
     * 移除session
     * <p>
     * 不会立即移除,等待定时器删除
     */
    public void removeSession(String sessionId) {
        //添加到待删除列表
        deleteSet.add(sessionId);
    }

    /**
     * 移除多个session
     * <p>
     * 不会立即移除,等待定时器删除
     */
    public void removeSessions(List<String> sessionIds) {
        //添加到待删除列表
        deleteSet.addAll(sessionIds);
    }

    /**
     * 切换到固定节点
     */
    public void switchNode(PFSession pfSession, MarsNode marsNode) {
        log.info("切换节点，sessionId={},toNode={}", pfSession.sessionId(), marsNode.getNodePath());
        try {
            SwitchNodeMessage switchNodeMessage = new SwitchNodeMessage(pfSession.sessionId(), marsNode.getNodePath()
                    , pfSession.playerId);
            pfSession.send2Gate(switchNodeMessage);
            removeSession(pfSession.sessionId());
        } catch (Exception e) {
            log.warn("节点切换异常", e);
        }
    }

    /**
     * 随机切换到指定类型的节点
     */
    public MarsNode switchNode(PFSession pfSession, NodeType nodeType) {
        try {
            MarsNode marsNode = randomMarsNode(nodeType, pfSession.getAddress().getHost(), pfSession.playerId);
            switchNode(pfSession, marsNode);
            return marsNode;
        } catch (Exception e) {
            log.warn("节点切换异常", e);
//            e.printStackTrace();
        }
        return null;
    }

    /**
     * 随机切换到指定类型的节点
     */
    public MarsNode switchNode(PFSession pfSession, String nodePath) {
        try {
            MarsNode marsNode = getNode(nodePath);
            switchNode(pfSession, marsNode);
            return marsNode;
        } catch (Exception e) {
            log.warn("节点切换异常", e);
//            e.printStackTrace();
        }
        return null;
    }

    /**
     * 随机切换到指定类型的节点
     */
    public MarsNode switchNode(PFSession pfSession, NodeType nodeType, String ip, long playerId) {
        try {
            MarsNode marsNode = randomMarsNode(nodeType, ip, playerId);
            switchNode(pfSession, marsNode);
            return marsNode;
        } catch (Exception e) {
            log.warn("节点切换异常", e);
//            e.printStackTrace();
        }
        return null;
    }

    public String getNodePath() {
        return nodeManager.getMarNodePath(nodeConfig.getType(), nodeConfig.getName());
    }

    public ClusterClient getByNodeType(NodeType nodeType, String ip, long playerId) {
        MarsNode randomOneMarsNode = randomMarsNode(nodeType, ip, playerId);
        if (randomOneMarsNode == null) {
            return null;
        }
        return clusterClientMap.get(randomOneMarsNode);
    }

    public MarsNode randomMarsNode(NodeType nodeType, String ip, long playerId) {
        MarsNode marsNode = nodeManager.getMarNode(nodeType);
        if (marsNode == null || !marsNode.hasChildren()) {
            log.warn("node not found or not has children,tpye is {}", nodeType);
            return null;
        }
        return marsNode.randomOneMarsNodeWithWeight(ip, playerId);
    }

    /**
     * 获取所有网关节点
     */
    public List<ClusterClient> getAllGate() {
        List<ClusterClient> clusterClients = new ArrayList<>();
        clusterClientMap.values().forEach(clusterClient -> {
            if (NodeType.GATE.toString().equals(clusterClient.getType())) {
                clusterClients.add(clusterClient);
            }
        });
        return clusterClients;
    }

    public Map<MarsNode, ClusterClient> getClusterClientMap() {
        return clusterClientMap;
    }

    /**
     * 随机一个客户端节点
     */
    public ClusterClient randClientByType(NodeType nodeType) {
        String nodeTypeStr = nodeType.toString();
        Map.Entry<MarsNode, ClusterClient> en =
                clusterClientMap.entrySet().stream().filter(e -> nodeTypeStr.equals(e.getValue().getType())).findAny().orElse(null);
        if (en == null) {
            return null;
        }
        return en.getValue();
    }

    /**
     * 随机一个客户端节点
     */
    public ClusterClient randClientByType(NodeType nodeType, int gameMajorType) {
        String nodeTypeStr = nodeType.toString();
        Map.Entry<MarsNode, ClusterClient> en = clusterClientMap.entrySet().stream().filter(e -> {
            ClusterClient clusterClient = e.getValue();
            if (nodeTypeStr.equals(clusterClient.getType())) {
                int[] gameMajorTypes = clusterClient.nodeConfig.gameMajorTypes;
                if (gameMajorTypes != null && clusterClient.nodeConfig.weight > 0 &&
                        (clusterClient.nodeConfig.getWhiteIpList() == null || clusterClient.nodeConfig.getWhiteIpList().length < 1) &&
                        (clusterClient.nodeConfig.getWhiteIdList() == null || clusterClient.nodeConfig.getWhiteIdList().length < 1)) {

                    for (int mType : gameMajorTypes) {
                        if (mType == gameMajorType) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }).findAny().orElse(null);

        if (en == null) {
            return null;
        }
        return en.getValue();
    }

    /**
     * 随机一个客户端节点
     */
    public ClusterClient randClientByTypeExcept(NodeType nodeType, String name) {
        String nodeTypeStr = nodeType.toString();
        Map.Entry<MarsNode, ClusterClient> en =
                clusterClientMap.entrySet().stream().filter(e -> nodeTypeStr.equals(e.getValue().getType()) && !e.getValue().nodeConfig.getName().equals(name)).findAny().orElse(null);
        if (en == null) {
            return null;
        }
        return en.getValue();
    }

    /**
     * 获取所有除网关外的节点
     */
    public List<ClusterClient> getAllExceptGate() {
        return getAllExcept(NodeType.GATE);
    }

    /**
     * 获取所有除...外的节点
     */
    public List<ClusterClient> getAllExcept(NodeType nodeType) {
        List<ClusterClient> clusterClients = new ArrayList<>();
        String name = nodeType.toString();
        for (Map.Entry<MarsNode, ClusterClient> en : clusterClientMap.entrySet()) {
            if (!name.equals(en.getValue().getType())) {
                clusterClients.add(en.getValue());
            }
        }
        return clusterClients;
    }

    /**
     * 获取该类型的所有节点
     */
    public List<ClusterClient> getNodesByType(NodeType nodeType) {
        List<ClusterClient> clusterClients = new ArrayList<>();
        String name = nodeType.toString();
        for (Map.Entry<MarsNode, ClusterClient> en : clusterClientMap.entrySet()) {
            if (name.equals(en.getValue().getType())) {
                clusterClients.add(en.getValue());
            }
        }
        return clusterClients;
    }

    /**
     * 获取该类型的所有节点
     *
     * @param nodeType 节点类型
     * @param gameType 游戏类型
     */
    public List<ClusterClient> getNodesByType(NodeType nodeType, int gameType) {
        return getNodesByType(nodeType, gameType, false);
    }

    /**
     * 获取该类型的所有节点
     *
     * @param nodeType 节点类型
     * @param gameType 游戏类型
     */
    public List<ClusterClient> getNodesByTypeExcludeSelf(NodeType nodeType, int gameType) {
        return getNodesByType(nodeType, gameType, true);
    }

    /**
     * 获取该类型的所有节点
     *
     * @param nodeType    节点类型
     * @param gameType    游戏类型
     * @param excludeSelf 是否排除本节点
     */
    public List<ClusterClient> getNodesByType(NodeType nodeType, int gameType, boolean excludeSelf) {
        List<ClusterClient> clusterClients = new ArrayList<>();
        String name = nodeType.toString();

        int gameMajorType = CommonUtil.getMajorTypeByGameType(gameType);
        for (Map.Entry<MarsNode, ClusterClient> en : clusterClientMap.entrySet()) {
            ClusterClient client = en.getValue();
            if (!name.equals(client.getType())) {
                continue;
            }

            if (client.nodeConfig.gameMajorTypes == null || client.nodeConfig.gameMajorTypes.length < 1) {
                continue;
            }

            //排除本节点
            if (excludeSelf && client.nodeConfig.getName().equals(this.nodeConfig.getName())) {
                continue;
            }

            for (int mType : client.nodeConfig.gameMajorTypes) {
                if (mType != gameMajorType) {
                    continue;
                }
                clusterClients.add(client);
                break;
            }
        }
        return clusterClients;
    }

    /**
     * 根据名称获取节点
     *
     * @param nodeName 节点名称
     */
    public ClusterClient getNodesByName(String nodeName) {
        for (Map.Entry<MarsNode, ClusterClient> en : clusterClientMap.entrySet()) {
            String name = en.getValue().nodeConfig.getName();
            if (nodeName.equals(name)) {
                return en.getValue();
            }
        }
        return null;
    }

    /**
     * 是否有该节点
     *
     * @param nodeType 节点类型
     * @param gameType 游戏类型
     */
    public boolean hasNodes(NodeType nodeType, int gameType) {
        String name = nodeType.toString();
        int gameMajorType = CommonUtil.getMajorTypeByGameType(gameType);
        return clusterClientMap.entrySet().stream().anyMatch(en -> {
            if (!name.equals(en.getValue().getType())) {
                return false;
            }

            int[] gameMajorTypes = en.getValue().nodeConfig.gameMajorTypes;
            if (gameMajorTypes == null || gameMajorTypes.length < 1) {
                return false;
            }

            for (int mType : gameMajorTypes) {
                if (mType == gameMajorType) {
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * 获取该类型的所有节点
     *
     * @param nodeType 节点类型
     * @param gameType 游戏类型
     */
    public Map<String, ClusterClient> getNodesMapByType(NodeType nodeType, int gameType) {
        Map<String, ClusterClient> map = new HashMap<>();
        String name = nodeType.toString();
        int gameMajorType = CommonUtil.getMajorTypeByGameType(gameType);
        for (Map.Entry<MarsNode, ClusterClient> en : clusterClientMap.entrySet()) {
            if (!name.equals(en.getValue().getType())) {
                continue;
            }

            int[] gameMajorTypes = en.getValue().nodeConfig.gameMajorTypes;
            if (gameMajorTypes == null || gameMajorTypes.length < 1) {
                continue;
            }

            for (int mType : gameMajorTypes) {
                if (mType != gameMajorType) {
                    continue;
                }
                map.put(en.getKey().getNodePath(), en.getValue());
                break;
            }
        }
        return map;
    }

    public MarsNode getNode(String path) {
        if (path == null) {
            log.warn("节点查找异常，path=null");
            return null;
        }
        return marsCurator.getMarsNode(path);
    }

    public ClusterClient getClusterByPath(String path) {
        MarsNode marsNode = marsCurator.getMarsNode(path);
        if (marsNode == null) {
            log.warn("node not found 1 ,path is {}", path);
            return null;
        }
        return clusterClientMap.get(marsNode);
    }

    public ConnectPool<NettyConnect<Object>> getMarsConnectPool(NetAddress netAddress) {
        return new ConnectPool(netAddress, nodeConfig.getTcpAddress(), marsCurator.getStartClientNetAddress(),
                initializer).init().start(timerCenter);
    }

    public void startClusterServer() {
        NetAddress netAddress = nodeConfig.getTcpAddress();
        if (netAddress != null) {
            NettyServer nettyServer = new NettyServer(netAddress.getPort(), initializer);
            nettyServer.setName("tcp-nio-" + netAddress.getPort());
            nettyServer.start();
        }
    }

    private void nodeAdd(MarsNode marsNode) {
        ClusterClient clusterClient = clusterClientMap.get(marsNode);

        if (clusterClient != null) {
            clusterClient.shutdown();
        }

        clusterClientMap.put(marsNode, new ClusterClient(marsNode, this));
    }

    /**
     * 当节点被移除时
     */
    private void nodeRemove(MarsNode marsNode) {
        ClusterClient clusterClient = clusterClientMap.remove(marsNode);
        if (clusterClient != null) {
            clusterClient.shutdown();
        }
    }

    @Override
    public void nodeChange(NodeChangeType nodeChangeType, MarsNode marsNode) {
        log.debug("集群节点信息修改,nodePath={},nodeChangeType = {}", marsNode.getNodePath(), nodeChangeType);
        switch (nodeChangeType) {
            case NODE_ADD:
                nodeAdd(marsNode);
                //clusterClient.init(marsNode);
                break;
            case NODE_REMOVE:
                nodeRemove(marsNode);
                //marsCurator.removeMarsNodeListener(marsNode.getNodePath(), this);
                break;
        }
    }

    public void init(boolean onTimer, TimerCenter timerCenter) {
        this.timerCenter = timerCenter;

        writePidFile();

        system = this;
        startClusterServer();
        if (onTimer && timerCenter != null) {
            clusterSystemEvent =
                    new TimerEvent<>(this, "ClusterSystem", 1).setInitTime(10).withTimeUnit(TimeUnit.MINUTES);
            timerCenter.add(clusterSystemEvent);
        }
        //初始化删除session定时器
        deleteTimerEvent = new TimerEvent<>(this, "deleteTimerEvent", 1).setInitTime(1).withTimeUnit(TimeUnit.SECONDS);
        if (timerCenter != null) {
            timerCenter.add(deleteTimerEvent);
        } else {
            log.warn("timerCenter is null");
        }
    }

    /*@Override
    public void run(String... args) throws Exception {
        init();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        writePidFile();
    }
    */

    public void init(TimerCenter timerCenter) {
        init(true, timerCenter);
    }

    private void writePidFile() {
        if (created.compareAndSet(false, true)) {
            try {
                File pidFile = new File("PID");
                log.info("输出 PID 文件，file=" + pidFile.getAbsolutePath());
                new ApplicationPid().write(pidFile);
                pidFile.deleteOnExit();
            } catch (Exception ex) {
                String message = String.format("Cannot create pid file %s",
                        "PID");
                log.warn(message, ex);
            }
        }
    }

    public int clusterSessionSize() {
        return sessionMap.size();
    }

    @Override
    public void onTimer(TimerEvent<String> e) {
        if (e == clusterSystemEvent) {
            log.info("节点权重={},当前session数量={}", nodeConfig.weight, clusterSessionSize());
            if (nodeConfig.weight == 0 && sessionMap.isEmpty()) {
                System.exit(0);
            }
        } else if (e == deleteTimerEvent) {
            deleteSession();
        }
    }

    /**
     * 删除无效session
     */
    private void deleteSession() {
        if (deleteSet.isEmpty()) {
            return;
        }
        Set<String> temp = new HashSet<>(deleteSet);
        deleteSet.clear();
        temp.forEach(sessionId -> {
            PFSession pfSession = sessionMap.remove(sessionId);
            if (pfSession != null) {
                playerIdSessionMap.remove(pfSession.playerId);
            } else {
                log.warn("移除session[{}]时,pfSession为空,无法移除玩家id到sessionId的映射", sessionId);
            }
        });
        log.debug("移除 无效session {} 个", temp.size());
    }

    /**
     * 检测活跃时间是否处于活跃中
     *
     * @param currentTimeMillis 当前时间戳
     * @param activeTime        上次活跃时间戳
     * @return true 已经不活跃了 false 活跃中
     */
    private boolean checkActive(long currentTimeMillis, long activeTime) {
        return currentTimeMillis - activeTime > SESSION_TIME_OUT_MINUTES * TimeHelper.ONE_MINUTE_OF_MILLIS;
    }

    /**
     * 检查当前的会话是否依然活跃，并移除超时或无效的会话。
     *
     * @param currentTimeMillis 当前时间戳，用于与会话的最后活跃时间进行比较
     * @param keys              玩家ID列表，用于检测相关会话是否需要被移除
     */
    public void checkSessionActive(long currentTimeMillis, List<Long> keys) {
        List<String> removeIds = new ArrayList<>();
        this.sessionMap.values().forEach(pfSession -> {
            //如果session 长时间没有活跃，检查玩家是否还在线
            if (checkActive(currentTimeMillis, pfSession.activeTime)) {
                if (pfSession.getReference() == null) {
                    log.warn("移除无效session，playerId={},sessionId={}", pfSession.getPlayerId(), pfSession.sessionId());
                    removeIds.add(pfSession.sessionId());
                }
            }
        });
        //根据被移除的PlayerSessionInfo的所有玩家id来反向检测session是否需要移除
        keys.forEach(playerId -> {
            PFSession pfSession = getSession(playerId);
            if (pfSession != null) {
                if (checkActive(currentTimeMillis, pfSession.activeTime)) {
                    log.warn("根据被移除的playerSession检测后, 移除Cluster无效session，playerId={},sessionId={}", pfSession.getPlayerId(), pfSession.sessionId());
                    removeIds.add(pfSession.sessionId());
                }
            }
        });
        removeSessions(removeIds);
    }

    /**
     * 通知所有的大厅和游戏节点
     */
    public void notifyHallAndGameNode(PFMessage pfMessage) {
        notifyNode(pfMessage, Set.of(NodeType.GATE.toString(), NodeType.HALL.toString())::contains);
    }

    /**
     * 通知节点
     */
    public void notifyNode(PFMessage pfMessage, Predicate<String> predicate) {
        //筛选符合条件的节点
        List<ClusterClient> clientList = clusterClientMap.values().stream().filter(clusterClient -> predicate.test(clusterClient.getType())).toList();
        sendClusterMessage(pfMessage, clientList);
    }

    /**
     * 通知所有节点
     */
    public void notifyAllNode(PFMessage pfMessage) {
        List<ClusterClient> clientList = clusterClientMap.values().stream().toList();
        sendClusterMessage(pfMessage, clientList);
    }

    /**
     * 向集群中的多个客户端发送消息。
     */
    private void sendClusterMessage(PFMessage pfMessage, List<ClusterClient> clientList) {
        if (clientList.isEmpty()) {
            return;
        }
        ClusterMessage msg = new ClusterMessage(pfMessage);
        clientList.forEach(clusterClient -> {
            try {
                clusterClient.write(msg);
                log.debug("给[{}]节点推送消息 node = {}", clusterClient.getType(), clusterClient.nodeConfig.getName());
            } catch (Exception e) {
                log.error("推送到[{}]节点信息异常 node = {}", clusterClient.getType(), clusterClient.nodeConfig.getName(), e);
            }
        });
    }

}
