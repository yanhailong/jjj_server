package com.jjg.game.common.curator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.cluster.ClusterHelper;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.config.ZookeeperConfig;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.monitor.FileLoader;
import com.jjg.game.common.monitor.FileMonitor;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.FileHelper;
import com.jjg.game.common.utils.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 节点管理
 * 负责节点配置，注册，加载，获取等
 *
 * @since 1.0
 */
@Component
@Order(3)
public class NodeManager implements MarsCuratorListener, MarsNodeListener, FileLoader {
    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    public NodeConfig nodeConfig;

    private MarsCurator marsCurator;
    @Autowired
    private ZookeeperConfig zkConfig;
    @Autowired
    public FileMonitor fileMonitor;

    private final String nodeConfigName = "config/nodeConfig.json";

    private String nodePath;

    public NodeManager() {
    }

    @Override
    public void marsCuratorRefreshed(MarsCurator marsCurator) {
//        File configFile = new File(nodeConfigName);
//        readConfig(configFile);

        register();
    }

    public void init(MarsCurator marsCurator) {
        this.marsCurator = marsCurator;
        obConfig();
    }

    public void obConfig() {
        fileMonitor.addFileObserver(nodeConfigName, this, true);
    }

    @Override
    public void load(File file, boolean isNew) {
        try {
            if (file.getName().endsWith(".swp")) {
                return;
            }
            log.info("on file change filename = {}，isNew={}", file.getName(), isNew);
            readConfig(file);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("on file change exception,filename = {},isNew={}", file.getName(), isNew, e);
        }
    }

    public void readConfig(File file) {
        String content = FileHelper.readFile(file, "UTF-8");
        if (StringUtils.isEmpty(content)) {
            log.debug("file content is null, filename = {}", file.getName());
            return;
        }

        JSONObject jsonObject = JSON.parseObject(content);

        Integer wight = jsonObject.getInteger("weight");
        if (wight != null) {
            nodeConfig.setWeight(wight);
        }

        Boolean open = jsonObject.getBoolean("open");
        if (open != null) {
            nodeConfig.setOpen(open);
        }

        Integer masterWeight = jsonObject.getInteger("masterWeight");
        if (masterWeight != null) {
            nodeConfig.setWeight(masterWeight);
        }

        JSONArray whiteIpArray = jsonObject.getJSONArray("whiteIpList");
        if (whiteIpArray != null) {
            nodeConfig.setWhiteIpList(whiteIpArray.toArray(new String[0]));
        }

        JSONArray whiteIdArray = jsonObject.getJSONArray("whiteIdList");
        if (whiteIdArray != null) {
            nodeConfig.setWhiteIdList(whiteIdArray.toArray(new String[0]));
        }
        update();
    }

    private void register() {
        try {
            String path = CoreConst.Common.SEPARATOR + nodeConfig.getParentPath() + CoreConst.Common.SEPARATOR +
                    nodeConfig.getType() + CoreConst.Common.SEPARATOR +
                    nodeConfig.getName();
            log.info("node register,path is {}", path);

            String nc = JSON.toJSONString(nodeConfig, true);
            nodePath = marsCurator.addPath(path, nc.getBytes(StandardCharsets.UTF_8), false);
        } catch (Exception e) {
            log.warn("node register fail.", e);
        }
    }

    public void update() {
        try {
            String path = CoreConst.Common.SEPARATOR +
                nodeConfig.getParentPath() +
                CoreConst.Common.SEPARATOR +
                nodeConfig.getType() +
                CoreConst.Common.SEPARATOR +
                nodeConfig.getName();
            log.info("node update,path is {}", path);

            String nc = JSON.toJSONString(nodeConfig, true);
            nodePath = marsCurator.updatePath(path, nc.getBytes(StandardCharsets.UTF_8), false);
            //marsCurator.addMarsNodeListener(path, this);
        } catch (Exception e) {
            log.warn("node update fail.", e);
        }
    }

    public MarsNode getMarNode(NodeType nodeType) {
        String nodePath =
            CoreConst.Common.SEPARATOR + zkConfig.getMarsRoot() + CoreConst.Common.SEPARATOR + nodeConfig.getParentPath()
                + CoreConst.Common.SEPARATOR + nodeType;
        return marsCurator.getMarsNode(nodePath);
    }

    public MarsNode getMarNode(String nodeType) {
        String nodePath =
            CoreConst.Common.SEPARATOR + zkConfig.getMarsRoot() + CoreConst.Common.SEPARATOR + nodeConfig.getParentPath()
                + CoreConst.Common.SEPARATOR + nodeType;
        return marsCurator.getMarsNode(nodePath);
    }

    public String getMarNodePath(String nodeType, String nodeName) {
        return CoreConst.Common.SEPARATOR + zkConfig.getMarsRoot() + CoreConst.Common.SEPARATOR + nodeConfig.getParentPath()
            + CoreConst.Common.SEPARATOR + nodeType + CoreConst.Common.SEPARATOR + nodeName;
    }

    /**
     * 加载游戏节点
     */
    public Pair<MarsNode, Boolean> getGameNodePairByWeight(int gameType, long playerId, String ip) {
        Pair<List<MarsNode>, Boolean> pairList = getGameNodeList(gameType, playerId, ip);
        if (pairList == null) {
            return null;
        }
        List<MarsNode> list = pairList.getFirst();
        int totalWeight = list.stream().mapToInt(m -> m.getNodeConfig().weight).sum();
        if (totalWeight <= 0) {
            log.error("所有的游戏节点权重和为0");
            return null;
        }

        int p = RandomUtils.randomInt(totalWeight);
        int sum = 0;
        for (MarsNode node : list) {
            sum += node.getNodeConfig().weight;
            if (p < sum) {
                return Pair.newPair(node, pairList.getSecond());
            }
        }
        log.error("逻辑应该不会到这");
        return null;
    }

    /**
     * 加载游戏节点
     */
    public MarsNode getGameNodeByWeight(int gameType, long playerId, String ip) {
        Pair<MarsNode, Boolean> marsNodeBooleanPair = getGameNodePairByWeight(gameType, playerId, ip);
        return marsNodeBooleanPair == null ? null : marsNodeBooleanPair.getFirst();
    }

    /**
     * 返回当前节点玩家数据是否在内存中
     *
     * @return ture 玩家数据在内存中 false玩家数据不在内存中
     */
    public boolean isPlayerDataInMemoryNode() {
        NodeType nodeType = NodeType.getNodeTypeByName(nodeConfig.getType());
        if (nodeType == NodeType.GAME && nodeConfig.getGameMajorTypes() != null) {
            for (int gameMajorType : nodeConfig.getGameMajorTypes()) {
                if (gameMajorType == CoreConst.GameMajorType.POKER || gameMajorType == CoreConst.GameMajorType.TABLE) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 获取所有的游戏节点
     *
     * @param gameType 游戏类型
     * @param playerId 玩家id
     * @param ip       ip
     * @return 节点列表,是否是白名单
     */
    public Pair<List<MarsNode>, Boolean> getGameNodeList(int gameType, long playerId, String ip) {
        NodeType nodeType = NodeType.GAME;
        MarsNode marsNode = getMarNode(nodeType);
        if (marsNode == null) {
            log.warn("查找游戏节点错误，playerplayerId={},nodeType={},gameType={}", playerId, nodeType, gameType);
            return null;
        }
        List<MarsNode> marsNodeList = marsNode.getAllChildren();
        if (marsNodeList == null || marsNodeList.isEmpty()) {
            log.warn("子节点为空，playerPlayerId={},nodeType={},gameType={}", playerId, nodeType, gameType);
            return null;
        }
        List<MarsNode> list = new ArrayList<>();
        List<MarsNode> preciselist = new ArrayList<>();
        boolean isWhitelist = false;
        int gameMajorType = CommonUtil.getMajorTypeByGameType(gameType);

        for (MarsNode node : marsNodeList) {
            NodeConfig nodeConfig = node.getNodeConfig();
            if (nodeConfig == null) {
                continue;
            }
            if (!nodeConfig.isOpen()) {
                continue;
            }
            if (nodeConfig.weight < 1) {
                continue;
            }
            if (!has(nodeConfig.gameMajorTypes, gameMajorType)) {
                continue;
            }
            if ((nodeConfig.whiteIdList == null || nodeConfig.whiteIdList.length == 0) && (nodeConfig.whiteIpList == null || nodeConfig.whiteIpList.length == 0)) {
                list.add(node);
            } else if ((ClusterHelper.preciseInIdWhiteList(playerId, nodeConfig.whiteIdList) || ClusterHelper.preciseInIpWhiteList(ip, nodeConfig.whiteIpList))) {
                preciselist.add(node);
            }
        }
        if (!preciselist.isEmpty()) {
            isWhitelist = true;
            list = preciselist;
        }
        return Pair.newPair(list, isWhitelist);
    }

    private boolean has(int[] arrays, int value) {
        if (arrays == null) {
            return false;
        }
        for (int v : arrays) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    public String getNodePath() {
        return nodePath;
    }

    @Override
    public void nodeChange(NodeChangeType nodeChangeType, MarsNode marsNode) {
        if (nodeChangeType == NodeChangeType.NODE_REMOVE && nodePath.equals(marsNode.getNodePath())) {
            log.warn("本节点被异常移除");
            register();
        }
    }
}
