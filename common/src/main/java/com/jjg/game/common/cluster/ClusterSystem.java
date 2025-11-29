package com.jjg.game.common.cluster;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.baselogic.function.SystemInterfaceHolder;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.curator.*;
import com.jjg.game.common.gate.GateClusterMessageDispatcher;
import com.jjg.game.common.listener.OnServerAutoShoutDown;
import com.jjg.game.common.listener.OnSwitchNode;
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
public class ClusterSystem implements MarsNodeListener, TimerListener<String>, OnSwitchNode {

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
            if (session != null && session.getConnect() != null && session.getConnect().isActive()) {
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
            log.info("添加sessionId:{}", sessionId);
            sessionMap.put(sessionId, pfSession);
            if (pfSession.playerId > 0) {
                playerIdSessionMap.put(pfSession.playerId, sessionId);
            }
        }
    }

    /**
     * 移除session
     */
    public void removeSession(String sessionId) {
        log.info("移除sessionId:{}", sessionId);
        PFSession pfSession = sessionMap.remove(sessionId);
        if (pfSession != null && pfSession.playerId > 0) {
            playerIdSessionMap.remove(pfSession.playerId, sessionId);
        }

    }


    /**
     * 切换到固定节点
     */
    public void switchNode(PFSession pfSession, MarsNode marsNode) {
        //如果要切换的节点和当前节点一致不切换
        if (nodeManager.getNodePath().equals(marsNode.getNodePath())) {
            log.info("相同节点名不切换节点，sessionId={},currentNode={} ,toNode={}", pfSession.sessionId(), nodeConfig.getName(),
                    marsNode.getNodeConfig().getName());
            return;
        }
        log.info("切换节点，sessionId={},toNode={}", pfSession.sessionId(), marsNode.getNodePath());
        try {
            SwitchNodeMessage switchNodeMessage = new SwitchNodeMessage(pfSession.sessionId(), marsNode.getNodePath()
                    , pfSession.playerId);
            pfSession.send2Gate(switchNodeMessage);
            onSwitchNodeAfter(pfSession);
        } catch (Exception e) {
            log.warn("节点切换异常", e);
        }
    }

    private void onSwitchNodeAfter(PFSession pfSession) {
        List<OnSwitchNode> gameSysInterface = SystemInterfaceHolder.getGameSysInterface(OnSwitchNode.class);
        if (CollectionUtil.isEmpty(gameSysInterface)) {
            for (OnSwitchNode event : gameSysInterface) {
                try {
                    event.OnSwitchNodeAction(pfSession);
                } catch (Exception e) {
                    log.error("节点选择事件处理异常 pfSession:{}", pfSession == null ? "null" : pfSession.sessionId(), e);
                }
            }
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
    }


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
            if (nodeConfig.waitClose() && sessionMap.isEmpty()) {
                boolean canExit = true;
                List<OnServerAutoShoutDown> gameSysInterface = SystemInterfaceHolder.getGameSysInterface(OnServerAutoShoutDown.class);
                for (OnServerAutoShoutDown shout : gameSysInterface) {
                    try {
                        boolean temp = shout.canExit();
                        canExit &= temp;
                    } catch (Exception exception) {
                        log.error("System exit error ", exception);
                    }
                }
                if (canExit) {
                    System.exit(0);
                }
            }
        }
    }

    /**
     * 检测活跃时间是否处于活跃中
     *
     * @param currentTimeMillis 当前时间戳
     * @param activeTime        上次活跃时间戳
     * @return true 已经不活跃了 false 活跃中
     */
    private boolean checkActive(long currentTimeMillis, long activeTime) {
//        return currentTimeMillis - activeTime > SESSION_TIME_OUT_MINUTES * TimeHelper.ONE_MINUTE_OF_MILLIS;
        return false;
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
    public void sendClusterMessage(PFMessage pfMessage, List<ClusterClient> clientList) {
        if (clientList.isEmpty()) {
            return;
        }
        ClusterMessage msg = new ClusterMessage(pfMessage);
        clientList.forEach(clusterClient -> {
            try {
                clusterClient.write(msg);
                log.debug("给[{}]节点推送消息 node = {},cmd = 0x{}", clusterClient.getType(), clusterClient.nodeConfig.getName(), Integer.toHexString(pfMessage.cmd));
            } catch (Exception e) {
                log.error("推送到[{}]节点信息异常 node = {},cmd = 0x{}", clusterClient.getType(), clusterClient.nodeConfig.getName(), Integer.toHexString(pfMessage.cmd), e);
            }
        });
    }

    /**
     * 获取当前节点所有的在线玩家id
     */
    public Set<Long> getAllOnlinePlayerId() {
        return playerIdSessionMap.keySet();
    }

    /**
     * 获取当前节点所有的在线玩家
     */
    public List<PFSession> getAllOnlinePlayerPFSession() {
        return new ArrayList<>(sessionMap.values());
    }

    @Override
    public void OnSwitchNodeAction(PFSession pfSession) {
        if (pfSession == null) {
            log.error("OnSwitchNodeAction pfSession is null");
            return;
        }
        removeSession(pfSession.sessionId());
    }
}
