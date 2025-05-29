package com.vegasnight.game.common.cluster;

import com.vegasnight.game.common.config.NodeConfig;
import com.vegasnight.game.common.curator.*;
import com.vegasnight.game.common.gate.GateClusterMessageDispacher;
import com.vegasnight.game.common.message.SwitchNodeMessage;
import com.vegasnight.game.common.net.Connect;
import com.vegasnight.game.common.net.NetAddress;
import com.vegasnight.game.common.netty.ConnectPool;
import com.vegasnight.game.common.netty.NettyServer;
import com.vegasnight.game.common.protostuff.PFSession;
import com.vegasnight.game.common.timer.TimerCenter;
import com.vegasnight.game.common.timer.TimerEvent;
import com.vegasnight.game.common.timer.TimerListener;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 集群系统总线
 * <p>
 * @since 1.0
 */
@Component
@Order(1)
//public class ClusterSystem implements MarsNodeListener, ApplicationListener<ContextRefreshedEvent>, CommandLineRunner, TimerListener {
public class ClusterSystem implements MarsNodeListener, TimerListener {

    public static ClusterSystem system;

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 集群各个节点客户端对象
     */
    private Map<MarsNode, ClusterClient> clusterClientMap = new HashMap<>();

    /**
     * 玩家session集合
     */
    private Map<String, PFSession> sessionMap = new ConcurrentHashMap<>();

    private ClusterMessageDispacher clusterMessageDispacher;

    /**
     * 连接通道初始化器
     */
    private ClusterConnectInitializer initializer;

    /**
     * 多线程连接通道初始化器
     */
    private ClusterConnectWorkPoolInitializer workPoolInitializer;
    /**
     * 微服务消息节点索引
     */
    private Map<Integer, List<MarsNode>> micserviceIndexs = new HashMap<>();

    private TimerEvent<String> clusterSystemEvent;


    @Autowired
    public NodeManager nodeManager;
    @Autowired
    public MarsCurator marsCurator;
    @Autowired
    public NodeConfig nodeConfig;

    public TimerCenter timerCenter;

    @Bean
    public ClusterMessageDispacher clusterMessageDispacher() {
        if (NodeType.GATE.toString().equals(nodeManager.nodeConfig.getType())) {
            return clusterMessageDispacher = new GateClusterMessageDispacher(this);
        } else {
            return clusterMessageDispacher = new ClusterMessageDispacher(this);
        }
    }

    @Bean
    public ClusterConnectInitializer createClusterConnectInitializer() {
        initializer = new ClusterConnectInitializer(clusterMessageDispacher);
        workPoolInitializer = new ClusterConnectWorkPoolInitializer(clusterMessageDispacher);
        return initializer;
    }

    public Map<String, PFSession> sessionMap() {
        return sessionMap;
    }

    /**
     * 移除session
     *
     * @param sessionId
     * @return
     */
    public PFSession removeSession(String sessionId) {

        return sessionMap.remove(sessionId);
    }

    /**
     * 切换到固定节点
     *
     * @param marsNode
     */
    public void switchNode(PFSession pfSession, MarsNode marsNode) {
        log.info("切换节点，sessionId={},toNode={}", pfSession.sessionId(), marsNode.getNodePath());
        try {
            SwitchNodeMessage switchNodeMessage = new SwitchNodeMessage(pfSession.sessionId(), marsNode.getNodePath(), pfSession.playerId);
            pfSession.send2Gate(switchNodeMessage);
            sessionMap.remove(pfSession.sessionId());
        } catch (Exception e) {
            log.warn("节点切换异常", e);
//            e.printStackTrace();
        }
    }

    /**
     * 随机切换到指定类型的节点
     *
     * @param nodeType
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
     *
     * @param nodePath
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
     * @return
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
     * @return
     */
    public ClusterClient randClientByType(NodeType nodeType) {
        String nodeTypeStr = nodeType.toString();
        Map.Entry<MarsNode, ClusterClient> en = clusterClientMap.entrySet().stream().filter(e -> nodeTypeStr.equals(e.getValue().getType())).findAny().orElse(null);
        if(en == null){
            return null;
        }
        return en.getValue();
    }

    /**
     * 随机一个客户端节点
     * @return
     */
    public ClusterClient randClientByType(NodeType nodeType,int gameType) {
        String nodeTypeStr = nodeType.toString();
        Map.Entry<MarsNode, ClusterClient> en = clusterClientMap.entrySet().stream().filter(e -> {
            if(nodeTypeStr.equals(e.getValue().getType())){
                int[] gameTypes = e.getValue().nodeConfig.gameTypes;
                if(gameTypes != null && gameTypes.length > 0){
                    for(int gType : gameTypes){
                        if(gType == gameType){
                            return true;
                        }
                    }
                }
            }
            return false;
        }).findAny().orElse(null);

        if(en == null){
            return null;
        }
        return en.getValue();
    }

    /**
     * 随机一个客户端节点
     * @return
     */
    public ClusterClient randClientByTypeExcept(NodeType nodeType,String name) {
        String nodeTypeStr = nodeType.toString();
        Map.Entry<MarsNode, ClusterClient> en = clusterClientMap.entrySet().stream().filter(e -> nodeTypeStr.equals(e.getValue().getType()) && !e.getValue().nodeConfig.getName().equals(name)).findAny().orElse(null);
        if(en == null){
            return null;
        }
        return en.getValue();
    }

    /**
     * 获取所有除网关外的节点
     * @return
     */
    public List<ClusterClient> getAllExceptGate() {
        return getAllExcept(NodeType.GATE);
    }

    /**
     * 获取所有除...外的节点
     * @return
     */
    public List<ClusterClient> getAllExcept(NodeType nodeType) {
        List<ClusterClient> clusterClients = new ArrayList<>();
        String name = nodeType.toString();
        for(Map.Entry<MarsNode,ClusterClient> en : clusterClientMap.entrySet()){
            if(!name.equals(en.getValue().getType())){
                clusterClients.add(en.getValue());
            }
        }
        return clusterClients;
    }

    /**
     * 获取该类型的所有节点
     * @return
     */
    public List<ClusterClient> getNodesByType(NodeType nodeType) {
        List<ClusterClient> clusterClients = new ArrayList<>();
        String name = nodeType.toString();
        for(Map.Entry<MarsNode,ClusterClient> en : clusterClientMap.entrySet()){
            if(name.equals(en.getValue().getType())){
                clusterClients.add(en.getValue());
            }
        }
        return clusterClients;
    }

    /**
     * 获取该类型的所有节点
     * @param nodeType  节点类型
     * @param gameType  游戏类型
     * @return
     */
    public List<ClusterClient> getNodesByType(NodeType nodeType,int gameType) {
        List<ClusterClient> clusterClients = new ArrayList<>();
        String name = nodeType.toString();
        for(Map.Entry<MarsNode,ClusterClient> en : clusterClientMap.entrySet()){
            if(!name.equals(en.getValue().getType())){
                continue;
            }

            int[] gameTypes = en.getValue().nodeConfig.gameTypes;
            if(gameTypes == null || gameTypes.length < 1){
                continue;
            }

            for(int gType : gameTypes){
                if(gType != gameType){
                    continue;
                }
                clusterClients.add(en.getValue());
                break;
            }
        }
        return clusterClients;
    }

    /**
     * 根据名称获取节点
     * @param nodeName  节点名称
     * @return
     */
    public ClusterClient getNodesByName(String nodeName) {
        for(Map.Entry<MarsNode,ClusterClient> en : clusterClientMap.entrySet()){
            String name = en.getValue().nodeConfig.getName();
            if(nodeName.equals(name)){
                return en.getValue();
            }
        }
        return null;
    }

    /**
     * 是否有该节点
     * @param nodeType  节点类型
     * @param gameType  游戏类型
     * @return
     */
    public boolean hasNodes(NodeType nodeType,int gameType) {
        String name = nodeType.toString();
        return clusterClientMap.entrySet().stream().anyMatch(en -> {
            if(!name.equals(en.getValue().getType())){
                return false;
            }

            int[] gameTypes = en.getValue().nodeConfig.gameTypes;
            if(gameTypes == null || gameTypes.length < 1){
                return false;
            }

            for(int gType : gameTypes){
                if(gType == gameType){
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * 获取该类型的所有节点
     * @param nodeType  节点类型
     * @param gameType  游戏类型
     * @return
     */
    public Map<String,ClusterClient> getNodesMapByType(NodeType nodeType,int gameType) {
        Map<String,ClusterClient> map = new HashMap<>();
        String name = nodeType.toString();
        for(Map.Entry<MarsNode,ClusterClient> en : clusterClientMap.entrySet()){
            if(!name.equals(en.getValue().getType())){
                continue;
            }

            int[] gameTypes = en.getValue().nodeConfig.gameTypes;
            if(gameTypes == null || gameTypes.length < 1){
                continue;
            }

            for(int gType : gameTypes){
                if(gType != gameType){
                    continue;
                }
                map.put(en.getKey().getNodePath(),en.getValue());
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

    public ConnectPool getMarsConnectPool(NetAddress netAddress) {
        return new ConnectPool(netAddress, nodeConfig.getTcpAddress(),marsCurator.getStartClientNetAddress(),initializer).init().start(timerCenter);
    }

    public void startClusterServer() {
        NetAddress netAddress = nodeConfig.getTcpAddress();
        if(netAddress != null){
            NettyServer nettyServer = new NettyServer(netAddress.getPort(), nodeConfig.workPool ? workPoolInitializer : initializer);
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

        //增加微服务索引
        Set<Integer> micMessageTypes = marsNode.getNodeConfig().getMicserviceMessageTypes();
        if (micMessageTypes != null && !micMessageTypes.isEmpty()) {
            micMessageTypes.forEach(messageType -> {
                List<MarsNode> marsNodes = micserviceIndexs.computeIfAbsent(messageType, m -> new ArrayList<>());
                if (!marsNodes.contains(marsNode)) {
                    marsNodes.add(marsNode);
                }
            });
        }
    }

    /**
     * 当节点被移除时
     *
     * @param marsNode
     */
    private void nodeRemove(MarsNode marsNode) {
        ClusterClient clusterClient = clusterClientMap.remove(marsNode);
        if (clusterClient != null) {
            clusterClient.shutdown();
        }
        //删除微服务索引
        Set<Integer> micMessageTypes = marsNode.getNodeConfig().getMicserviceMessageTypes();
        if (micMessageTypes != null && !micMessageTypes.isEmpty()) {
            micMessageTypes.forEach(messageType -> {
                List<MarsNode> marsNodes = micserviceIndexs.get(messageType);
                if (marsNodes != null && marsNodes.contains(marsNode)) {
                    marsNodes.remove(marsNode);
                    if (marsNodes.isEmpty()) {
                        micserviceIndexs.remove(messageType);
                    }
                }
            });
        }
    }

    /**
     * 微服务消息分配
     *
     * @param currentClient
     * @param messageType
     * @return
     * @throws InterruptedException
     */
    public Connect micserviceAllot(ClusterClient currentClient, int messageType) throws InterruptedException {
        List<MarsNode> micserviceNodes = micserviceIndexs.get(messageType);
        if (micserviceNodes == null || micserviceNodes.isEmpty()) {
            return null;
        }
        //如果当前用户所在连接，可以处理该微服务消息，则不选择其他连接
        if (micserviceNodes.contains(currentClient.marsNode)) {
            return null;
        }
        int index = (int) (Math.random() * micserviceNodes.size());
        MarsNode marsNode = micserviceNodes.get(index);
        ClusterClient clusterClient = clusterClientMap.get(marsNode);
        if (clusterClient != null) {
            return clusterClient.getConnect();
        }
        return null;
    }

    @Override
    public void nodeChange(NodeChangeType nodeChangeType, MarsNode marsNode) {
        log.debug("集群节点信息修改,nodePath={}", marsNode.getNodePath());
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

    public void init(boolean onTimer,TimerCenter timerCenter){
        this.timerCenter = timerCenter;

        writePidFile();

        system = this;
        startClusterServer();
        if (onTimer && timerCenter != null) {
            clusterSystemEvent = new TimerEvent<>(this, "ClusterSystem", 1).setInitTime(10).withTimeUnit(TimeUnit.MINUTES);
            timerCenter.add(clusterSystemEvent);
        }
    }

    public void init(TimerCenter timerCenter){
        init(true,timerCenter);
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

    private static final AtomicBoolean created = new AtomicBoolean(false);

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

    public int cluserSessionSize(){
        return sessionMap.size();
    }

    @Override
    public void onTimer(TimerEvent e) {
        if (e == clusterSystemEvent) {
            log.info("节点权重={},当前session数量={}", nodeConfig.weight, cluserSessionSize());
            if (nodeConfig.weight == 0 && sessionMap.isEmpty()) {
                System.exit(0);
            }
        }
    }

}
