package com.jjg.game.common.curator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import org.apache.commons.lang3.StringUtils;
import com.jjg.game.common.cluster.ClusterHelper;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.config.ZookeeperConfig;
import com.jjg.game.common.utils.FileHelper;
import com.jjg.game.common.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 节点管理
 * 负责节点配置，注册，加载，获取等
 * @since 1.0
 */
@Component
@Order(3)
public class NodeManager implements MarsCuratorListener, MarsNodeListener {
    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    public NodeConfig nodeConfig;

    private MarsCurator marsCurator;
    @Autowired
    private ZookeeperConfig zkConfig;

    public String nodePath;

    public NodeManager() {
    }

    @Override
    public void marsCuratorRefreshed(MarsCurator marsCurator) {
        register();
    }

    public void init(MarsCurator marsCurator){
        this.marsCurator = marsCurator;
    }

    public void readConfig(File file) {
        String content = FileHelper.readFile(file, "UTF-8");
        if(StringUtils.isEmpty(content)){
            log.debug("file content is null, filename = {}",file.getName());
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
            StringBuilder sb = new StringBuilder(CoreConst.SEPARATOR);
            sb.append(nodeConfig.getParentPath()).append(CoreConst.SEPARATOR)
                    .append(nodeConfig.getType()).append(CoreConst.SEPARATOR)
                    .append(nodeConfig.getName());
            String path = sb.toString();
            log.info("node register,path is {}", path);

            String nc = JSON.toJSONString(nodeConfig, true);
            nodePath = marsCurator.addPath(path, nc.getBytes("UTF-8"), false);
        } catch (Exception e) {
            log.warn("node register fail.", e);
        }
    }

    public void update() {
        try {
            StringBuilder sb = new StringBuilder(CoreConst.SEPARATOR);
            sb.append(nodeConfig.getParentPath()).append(CoreConst.SEPARATOR)
                    .append(nodeConfig.getType()).append(CoreConst.SEPARATOR)
                    .append(nodeConfig.getName());
            String path = sb.toString();
            log.info("node update,path is {}", path);

            String nc = JSON.toJSONString(nodeConfig, true);
            nodePath = marsCurator.updatePath(path, nc.getBytes("UTF-8"), false);
            //marsCurator.addMarsNodeListener(path, this);
        } catch (Exception e) {
            log.warn("node update fail.", e);
        }
    }

    public MarsNode getMarNode(NodeType nodeType) {
        String nodePath = CoreConst.SEPARATOR + zkConfig.getMarsRoot() + CoreConst.SEPARATOR + nodeConfig.getParentPath()
                + CoreConst.SEPARATOR + nodeType;
        return marsCurator.getMarsNode(nodePath);
    }

    public MarsNode getMarNode(String nodeType) {
        String nodePath = CoreConst.SEPARATOR + zkConfig.getMarsRoot() + CoreConst.SEPARATOR + nodeConfig.getParentPath()
                + CoreConst.SEPARATOR + nodeType;
        return marsCurator.getMarsNode(nodePath);
    }

    public String getMarNodePath(String nodeType, String nodeName) {
        return CoreConst.SEPARATOR + zkConfig.getMarsRoot() + CoreConst.SEPARATOR + nodeConfig.getParentPath()
                + CoreConst.SEPARATOR + nodeType + CoreConst.SEPARATOR + nodeName;
    }

    /**
     * 加载游戏节点
     * @param nodeType
     * @param gameType
     * @param playerId
     * @param ip
     * @return
     */
    public MarsNode loadGameNode(NodeType nodeType, int gameType, long playerId, String ip) {
        MarsNode marsNode = getMarNode(nodeType);
        if (marsNode == null) {
            log.warn("查找游戏节点错误，playerplayerId={},nodeType={},gameType={}", playerId,nodeType, gameType);
            return null;
        }
        List<MarsNode> marsNodeList = marsNode.getAllChildren();
        if (marsNodeList == null || marsNodeList.isEmpty()) {
            log.warn("子节点为空，playerPlayerId={},nodeType={},gameType={}", playerId,nodeType, gameType);
            return null;
        }
        List<MarsNode> list = new ArrayList<>();
        List<MarsNode> preciselist = new ArrayList<>();
        for (MarsNode node : marsNodeList) {
            NodeConfig nodeConfig = node.getNodeConfig();
            if (nodeConfig == null) {
                System.out.println(1);
                continue;
            }
            if(!nodeConfig.isOpen()){
                System.out.println(2);
                continue;
            }
            if(nodeConfig.weight < 1){
                System.out.println(3);
                continue;
            }
            if(!has(nodeConfig.gameTypes, gameType)){
                System.out.println(4);
                continue;
            }
            if ((nodeConfig.whiteIdList == null || nodeConfig.whiteIdList.length == 0) && (nodeConfig.whiteIpList == null || nodeConfig.whiteIpList.length == 0)) {
                list.add(node);
            } else if ((ClusterHelper.preciseInIdWhiteList(playerId, nodeConfig.whiteIdList) || ClusterHelper.preciseInIpWhiteList(ip, nodeConfig.whiteIpList))) {
                preciselist.add(node);
            }
        }
        if (!preciselist.isEmpty()) {
            list = preciselist;
        }
        if (list.isEmpty()) {
            System.out.println(5);
            return null;
        }
        if(list.size() == 1){
            return list.get(0);
        }

        int totalWeight = list.stream().mapToInt(m -> m.getNodeConfig().weight).sum();
        if (totalWeight <= 0) {
            System.out.println(6);
            return null;
        }

        int p = RandomUtils.randomInt(totalWeight);
        int sum = 0;
        for (MarsNode node : list) {
            sum += node.getNodeConfig().weight;
            if (p < sum) {
                return node;
            }
        }
        System.out.println(7);
        return null;
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

    @Override
    public void nodeChange(NodeChangeType nodeChangeType, MarsNode marsNode) {
        if(nodeChangeType == NodeChangeType.NODE_REMOVE && nodePath.equals(marsNode.getNodePath())){
            log.warn("本节点被异常移除");
            register();
        }
    }
}
