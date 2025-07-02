package com.jjg.game.common.curator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.listener.IGameClusterLeaderListener;
import com.jjg.game.common.utils.CommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.config.ZookeeperConfig;
import com.jjg.game.common.curator.MarsNodeListener.NodeChangeType;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import com.jjg.game.common.net.NetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * zookeeper 监视器管理类
 *
 * @scene 1.0
 */
@Order(3)
@Component
public class MarsCurator implements TreeCacheListener {
    public static final String ALL = "ALL";
    private Logger log = LoggerFactory.getLogger(getClass());

    private CuratorFramework client = null;

    @Autowired
    private ZookeeperConfig zkConfig;
    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private NodeManager nodeManager;

    //根节点目录
    private String rootPath;
    //节点路径
    public String nodePath;

    private MarsNode marsRootNode;

    private TreeCache treeCache;

    private Set<MarsCuratorListener> listeners = new CopyOnWriteArraySet<>();

    public Map<String, MarsNodeListener> marsNodeListeners;

    private ConcurrentHashMap<Integer, AtomicBoolean> masterMap = new ConcurrentHashMap<>();

    private NetAddress startClientNetAddress;
    private AtomicBoolean nodeInit = new AtomicBoolean();

    //节点队列
    private ConcurrentLinkedQueue<TreeCacheEvent> queue = new ConcurrentLinkedQueue<>();

    public MarsCurator() {
    }

    public void init(ApplicationContext context) {
        Map<String, MarsCuratorListener> tmpMap = context.getBeansOfType(MarsCuratorListener.class);
        if (tmpMap != null && !tmpMap.isEmpty()) {
            listeners = new CopyOnWriteArraySet<>(tmpMap.values());
        }
        this.marsNodeListeners = context.getBeansOfType(MarsNodeListener.class);
        startZKClient();
    }

    public void startZKClient() {
        try {
            log.debug("mars curator init. ");
            ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(
                zkConfig.getBaseSleepTimeMs(), zkConfig.getMaxRetries());

            client = CuratorFrameworkFactory.newClient(zkConfig.getConnects(),
                zkConfig.getSessionTimeoutMs(), zkConfig.getConnectionTimeoutMs(), retryPolicy);
            client.start();
            initRootPath();
            cacheMarsNode();
            leaderLatch();

        } catch (Exception e) {
            log.warn("mars curator init fail...", e);
        }
    }

    /**
     * 节点竞选
     */
    private void leaderLatch() {
        try {
            if (!nodeConfig.isMasterSelect()) {
                return;
            }

            Map<Integer, String> pathMap = new HashMap<>();
            if (NodeType.GAME.name().equals(nodeConfig.getType())) {
                int[] gameTypes = nodeConfig.getGameTypes();
                if (gameTypes == null || gameTypes.length < 1) {
                    log.warn("选举主节点失败,gameTypes错误 nodeName = {},nodeType = {},gameTypes={}", nodeConfig.getName(),
                        nodeConfig.getType(), gameTypes == null ? 0 : gameTypes.length);
                    return;
                }

                for (int i = 0; i < gameTypes.length; i++) {
                    String path =
                        mkPath("/" + nodeConfig.getParentPath()) + "/MASTER/" + nodeConfig.getType() + "/" + gameTypes[i];
                    pathMap.put(gameTypes[i], path);
                }
            } else if (NodeType.HALL.name().equals(nodeConfig.getType())) {
                String path = mkPath("/" + nodeConfig.getParentPath()) + "/MASTER/" + nodeConfig.getType();
                pathMap.put(NodeType.HALL.getValue(), path);
            }

            for (Map.Entry<Integer, String> en : pathMap.entrySet()) {
                int gameChildType = en.getKey();
                masterMap.put(gameChildType, new AtomicBoolean());

                String path = en.getValue();

                LeaderLatch latch = new LeaderLatch(client, path, nodeConfig.getName(),
                    LeaderLatch.CloseMode.NOTIFY_LEADER);
                latch.addListener(new LeaderLatchListener() {
                    @Override
                    public void isLeader() {
                        masterMap.computeIfPresent(gameChildType, (k, v) -> {
                            v.set(true);
                            return v;
                        });
                        Map<String, IGameClusterLeaderListener>
                            listenerMap = CommonUtil.getContext().getBeansOfType(IGameClusterLeaderListener.class);
                        try {
                            listenerMap.values().forEach(listener -> listener.isLeader(gameChildType));
                        } catch (Exception e) {
                            log.error("游戏节点：{} 游戏类型：{} 在选举成master时调用监听器发生异常", path, gameChildType, e);
                        }
                        nodeConfig.setWeight(1);
                        nodeManager.update();
                        log.info("该节点被选举为主节点 nodeName = {},nodeType = {},gameChildType={},wight={}",
                            nodeConfig.getName(), nodeConfig.getType(), gameChildType, nodeConfig.weight);
                    }

                    @Override
                    public void notLeader() {
                        masterMap.computeIfPresent(gameChildType, (k, v) -> {
                            v.set(false);
                            return v;
                        });
                        Map<String, IGameClusterLeaderListener>
                            listenerMap = CommonUtil.getContext().getBeansOfType(IGameClusterLeaderListener.class);
                        try {
                            listenerMap.values().forEach(listener -> listener.notLeader(gameChildType));
                        } catch (Exception e) {
                            log.error("游戏节点：{} 游戏类型：{} 在变成非master节点时调用监听器发生异常", path, gameChildType, e);
                        }
                        log.info("该节点离开，失去主节点身份 nodeName = {},gameChildType={},nodeType = {}", nodeConfig.getName(),
                            gameChildType, nodeConfig.getType());
                    }
                });
                latch.start();
            }
        } catch (Exception e) {
            log.error("启动节点竞选异常", e);
        }
    }

    public void stop() {
        try {
            if (treeCache != null) {
                treeCache.close();
            }
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            log.warn("mars curator stop fail.", e);
        }
    }

    public void restart() {
        try {
            stop();
            startZKClient();
        } catch (Exception e) {
            log.warn("restart fail.", e);
        }
    }

    public CuratorFramework getClient() {
        return client;
    }

    /**
     * 锁服务，获得一个指定路径的锁
     *
     * @param lockPath
     * @return
     */
    public InterProcessMutex getLock(String lockPath) {
        return new InterProcessMutex(client, lockPath);
    }

    public String addPath(String path, byte[] payload, boolean persistent) {
        nodePath = mkPath(path);
        try {
            if (!checkExists(nodePath)) {
                log.info("create path {}", nodePath);
                client.create()
                    .creatingParentsIfNeeded().withMode(persistent
                        ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL)
                    .forPath(nodePath, payload);
            }
        } catch (Exception e) {
            log.warn("add path fail.path is {}", path, e);
        }
        return nodePath;
    }

    public String updatePath(String path, byte[] payload, boolean persistent) {
        nodePath = mkPath(path);
        try {
            if (checkExists(nodePath)) {
                log.info("update path {}", nodePath);
                client.setData().forPath(nodePath, payload);
            }
        } catch (Exception e) {
            log.warn("update path fail.path is {}", path, e);
        }
        return nodePath;
    }

    /**
     * 执行其他监听节点变化的方法
     *
     * @param nodeChangeType
     * @param marsNode
     */
    private void notifyMarsNodeListener(NodeChangeType nodeChangeType, MarsNode marsNode) {
        String path = marsNode.getNodePath();

        if ((nodePath == null || !nodePath.equals(path)) && !rootPath.equals(path) && marsNode.getNodeConfig() != null) {
            marsNodeListeners.forEach((k, v) -> {
                v.nodeChange(nodeChangeType, marsNode);
                log.info("notify mars node listeners.path={},nodeChangeType={},listener={}", path, nodeChangeType,
                    v.getClass().getName());
            });
        }
    }


    public MarsNode getMarsNode(String path) {
        //path = mkPath(path);
        return marsRootNode.getChildren(path, true);
    }

    /**
     * 添加节点
     *
     * @param path
     * @param data
     * @return
     */
    private MarsNode addMarsNode(String path, String data) {
        MarsNode marsNode = new MarsNode(path, data);
        if (path.equals(rootPath)) {
            marsRootNode = marsNode;
        } else {
            marsRootNode.addChildren(marsNode);
        }
        notifyMarsNodeListener(NodeChangeType.NODE_ADD, marsNode);
        return marsNode;
    }

    /**
     * 更新节点
     *
     * @param path
     * @param data
     * @return
     */
    private MarsNode updateMarsNode(String path, String data) {
        MarsNode marsNode = marsRootNode.getChildren(path, true);
        if (marsNode != null) {
            marsNode.updateData(data);
        }
        return marsNode;
    }

    /**
     * 删除节点
     *
     * @param path
     * @return
     */
    private MarsNode removeMarsNode(String path) {
        MarsNode marsNode = marsRootNode.removeChildren(path);
        if (marsNode != null) {
            notifyMarsNodeListener(NodeChangeType.NODE_REMOVE, marsNode);
        } else {
            log.warn("[NODE_REMOVE],找不到指定的节点,path={}", path);
        }
        return marsNode;
    }

    private void cacheMarsNode() throws Exception {
        treeCache = new TreeCache(client, rootPath);

        treeCache.getListenable().addListener(this);
        treeCache.start();
    }

    private boolean checkExists(String path) throws Exception {
        Stat stat = client.checkExists().forPath(path);
        return stat != null;
    }

    private String mkPath(String path) {
        return rootPath + path;
    }

    private void initRootPath() {
        try {
            rootPath = CoreConst.Common.SEPARATOR + zkConfig.getMarsRoot();
            if (!checkExists(rootPath)) {
                log.debug("mars root path {} not exist.", rootPath);
                client.create().forPath(rootPath);
            }
        } catch (Exception e) {
            log.warn("mars root path init fail...", e);
        }
    }

    private void notifyRefreshed() {
        listeners.forEach(listener -> {
            try {
                listener.marsCuratorRefreshed(this);
            } catch (Exception e) {
                log.warn("notify refreshed error.", e);
            }
        });
    }

    /**
     * 监听 ZooKeeper 树形结构节点的变化
     *
     * @param client
     * @param event
     * @throws Exception
     */
    @Override
    public void childEvent(CuratorFramework client, TreeCacheEvent event)
        throws Exception {
        log.info("Event type = {}, client state = {}", event.getType(), client.getState());

        TreeCacheEvent.Type type = event.getType();
        ChildData childData = event.getData();
        if (childData == null) {
            log.debug("No data in event[{}]", event);
            switch (type) {
                case CONNECTION_LOST:
                    if (client.getZookeeperClient().blockUntilConnectedOrTimedOut()) {
//            case CONNECTION_SUSPENDED:
                        restart();
                    }
                    break;
                case CONNECTION_RECONNECTED:
                case INITIALIZED:
                    notifyRefreshed();

                    startClientNetAddress = nodeConfig.getTcpAddress();
                    nodeInit.set(true);
                    startMarsNodeEvent();
                    startClientNetAddress = null;
                    break;
            }
        } else {
            queue.offer(event);
            if (nodeInit.get()) {
                startMarsNodeEvent();
            }
        }
    }

    /**
     * 该节点是不是主节点, 如果是大厅则传入大厅的value值，如果是游戏的话则传入游戏类型
     */
    public boolean master(int gameChildType) {
        if (!masterMap.containsKey(gameChildType)) {
            return false;
        }
        return masterMap.get(gameChildType).get();
    }

    public NetAddress getStartClientNetAddress() {
        return startClientNetAddress;
    }

    /**
     * 处理节点变化事件
     */
    private synchronized void startMarsNodeEvent() {
        TreeCacheEvent event = null;
        while ((event = queue.poll()) != null) {
            try {
                TreeCacheEvent.Type type = event.getType();
                ChildData childData = event.getData();

                byte[] byteData = childData.getData();
                if (byteData == null) {
                    byteData = new byte[0];
                }
                String data = new String(byteData, "UTF-8");
                String path = childData.getPath();
                if (!add(data, path)) {
                    continue;
                }

                String strStat = JSON.toJSONString(childData.getStat());

                log.debug("Receive event: type=[{}],path={},data={},stat={}", type, path, data, strStat);
                switch (type) {
                    case NODE_ADDED:
                        addMarsNode(path, data);
                        break;
                    case NODE_UPDATED:
                        updateMarsNode(path, data);
                        break;
                    case NODE_REMOVED:
                        removeMarsNode(path);
                        break;
                }
            } catch (Exception e) {
                log.debug("", e);
            }
        }
    }

    private boolean add(String data, String path) {
        try {
            if (!this.nodeConfig.isSameNodeJoin()) {
                return true;
            }

            String[] pathArr = path.split("/");
            if (pathArr.length < 4) {
                return true;
            }
            String nodeType = pathArr[3];
            if (NodeType.HALL.name().equals(nodeType)) {
                return true;
            } else if (NodeType.GATE.name().equals(nodeType)) {
                //网关不需要和gm连接
                if (NodeType.GM.name().equals(this.nodeConfig.getType())) {
                    log.debug("网关不需要和gm连接1 thisNodeType = {},targetNodeType={}", this.nodeConfig.getType(), nodeType);
                    return false;
                }
                return true;
            } else if (NodeType.GAME.name().equals(nodeType)) {
                if (StringUtils.isEmpty(data)) {
                    return true;
                }

                if (data.length() < 50) {
                    return true;
                }

                if (NodeType.GATE.name().equals(this.nodeConfig.getType()) || NodeType.HALL.name().equals(this.nodeConfig.getType())) {
                    return true;
                }

                NodeConfig anotherNodeConfig = JSONObject.parseObject(data, NodeConfig.class);

                if (anotherNodeConfig.getGameTypes() != null && anotherNodeConfig.getGameTypes().length > 0
                    && this.nodeConfig.getGameTypes() != null && this.nodeConfig.getGameTypes().length > 0) {
                    for (int anotherGameType : anotherNodeConfig.getGameTypes()) {
                        for (int thisGameType : this.nodeConfig.getGameTypes()) {
                            if (anotherGameType == thisGameType) {
                                return true;
                            }
                        }
                    }
                }
                log.debug("这个节点不需要缓存  anotherNodeName={},gameTypes={}", anotherNodeConfig.getName(),
                    Arrays.toString(anotherNodeConfig.getGameTypes()));
                return false;
            } else if (NodeType.GM.name().equals(nodeType)) {
                //网关不需要和gm连接
                if (NodeType.GATE.name().equals(this.nodeConfig.getType())) {
                    log.debug("网关不需要和gm连接2 thisNodeType = {},targetNodeType={}", this.nodeConfig.getType(), nodeType);
                    return true;
                }
            }

            if ("master".equalsIgnoreCase(nodeType)) {
                if (pathArr.length < 5) {
                    return true;
                }
                String masterType = pathArr[4];
                if (!NodeType.GAME.name().equals(masterType)) {
                    return true;
                }

                if (pathArr.length < 6) {
                    return true;
                }
                String startStr = pathArr[5];
                if (!StringUtils.isNumeric(startStr)) {
                    return false;
                }
                Integer gameType = Integer.parseInt(startStr);
                for (int thisGameType : this.nodeConfig.getGameTypes()) {
                    if (gameType == thisGameType) {
                        return true;
                    }
                }
                log.debug("这个主节点不需要缓存  nodeType={},masterType={},startStr={}", nodeType, masterType, startStr);
                return false;
            }

            log.debug("这个节点不需要缓存  path={}", path);
            return false;
        } catch (Exception e) {
            log.error("", e);
            return true;
        }
    }
}
