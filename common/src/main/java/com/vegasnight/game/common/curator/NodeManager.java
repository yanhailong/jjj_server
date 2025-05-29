package com.vegasnight.game.common.curator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import com.vegasnight.game.common.cluster.ClusterHelper;
import com.vegasnight.game.common.config.NodeConfig;
import com.vegasnight.game.common.config.ZookeeperConfig;
import com.vegasnight.game.common.data.MarsConst;
import com.vegasnight.game.common.micservice.MicServiceManager;
import com.vegasnight.game.common.monitor.FileLoader;
import com.vegasnight.game.common.monitor.FileMonitor;
import com.vegasnight.game.common.utils.FileHelper;
import com.vegasnight.game.common.utils.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.vegasnight.game.common.data.MarsConst.SEPARATOR;

/**
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

    public String nodePath;

    public String configFile = "config/nodeConfig.json";
    @Autowired
    public FileMonitor fileMonitor;

    @Autowired
    public MicServiceManager micServiceManager;

    public NodeManager() {
    }

    @Override
    public void marsCuratorRefreshed(MarsCurator marsCurator) {
        register();
    }

    public void init(MarsCurator marsCurator){
        this.marsCurator = marsCurator;
        obConfig();
    }

    public void obConfig() {
        fileMonitor.addFileObserver(configFile, this, true);
    }

    @Override
    public void load(File file, boolean isNew) {
        try {
            if(file.getName().endsWith(".swp")){
                return;
            }
            log.info("on file change filename = {}，isNew={}", file.getName(), isNew);
            readConfig(file);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("on file change exception,filename = {},isNew={}" ,file.getName() ,isNew, e);
        }

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
            StringBuilder sb = new StringBuilder(SEPARATOR);
            sb.append(nodeConfig.getParentPath()).append(SEPARATOR)
                    .append(nodeConfig.getType()).append(SEPARATOR)
                    .append(nodeConfig.getName());
            String path = sb.toString();
            log.info("node register,path is {}", path);

            //添加微服务
            if (nodeConfig.showMicservice) {
                nodeConfig.setMicserviceMessageTypes(micServiceManager.messageTypes);
            }else{
                nodeConfig.setMicserviceMessageTypes(null);
            }

            String nc = JSON.toJSONString(nodeConfig, true);
            nodePath = marsCurator.addPath(path, nc.getBytes("UTF-8"), false);
        } catch (Exception e) {
            log.warn("node register fail.", e);
        }
    }

    public void update() {
        try {
            StringBuilder sb = new StringBuilder(SEPARATOR);
            sb.append(nodeConfig.getParentPath()).append(SEPARATOR)
                    .append(nodeConfig.getType()).append(SEPARATOR)
                    .append(nodeConfig.getName());
            String path = sb.toString();
            log.info("node update,path is {}", path);

            //添加微服务
            if (nodeConfig.showMicservice) {
                nodeConfig.setMicserviceMessageTypes(micServiceManager.messageTypes);
            }else{
                nodeConfig.setMicserviceMessageTypes(null);
            }

            String nc = JSON.toJSONString(nodeConfig, true);
            nodePath = marsCurator.updatePath(path, nc.getBytes("UTF-8"), false);
            //marsCurator.addMarsNodeListener(path, this);
        } catch (Exception e) {
            log.warn("node update fail.", e);
        }
    }

    public MarsNode getMarNode(NodeType nodeType) {
        String nodePath = MarsConst.SEPARATOR + zkConfig.getMarsRoot() + MarsConst.SEPARATOR + nodeConfig.getParentPath()
                + MarsConst.SEPARATOR + nodeType;
        return marsCurator.getMarsNode(nodePath);
    }

    public MarsNode getMarNode(String nodeType) {
        String nodePath = MarsConst.SEPARATOR + zkConfig.getMarsRoot() + MarsConst.SEPARATOR + nodeConfig.getParentPath()
                + MarsConst.SEPARATOR + nodeType;
        return marsCurator.getMarsNode(nodePath);
    }

    public String getMarNodePath(String nodeType, String nodeName) {
        return MarsConst.SEPARATOR + zkConfig.getMarsRoot() + MarsConst.SEPARATOR + nodeConfig.getParentPath()
                + MarsConst.SEPARATOR + nodeType + MarsConst.SEPARATOR + nodeName;
    }

    public MarsNode loadGameNode(NodeType nodeType, int gameType, long playerId, String ip) {
        return loadGameNode(nodeType, gameType, playerId, ip,null);
    }

    public MarsNode loadGameNode(NodeType nodeType, int gameType, long playerId, String ip, Map<String,BigDecimal> nodeUserNumMap) {
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
                continue;
            }
            if(!nodeConfig.isOpen()){
                continue;
            }
            if(nodeConfig.weight < 1){
                continue;
            }
            if(!has(nodeConfig.gameTypes, gameType)){
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
            return null;
        }
        if(list.size() == 1){
            return list.get(0);
        }

        int totalWeight = list.stream().mapToInt(m -> m.getNodeConfig().weight).sum();
        if (totalWeight <= 0) {
            return null;
        }

        if(nodeUserNumMap != null && nodeUserNumMap.size() > 0){
            BigDecimal allUserNumBigDecimal = BigDecimal.ZERO;
            for (MarsNode node : list) {
                BigDecimal nodeUserNumBigDecimal = nodeUserNumMap.get(node.getNodeConfig().getName());
                if(nodeUserNumBigDecimal == null){
                    return node;
                }
                allUserNumBigDecimal.add(nodeUserNumBigDecimal);
            }

            if(allUserNumBigDecimal.compareTo(BigDecimal.ONE) < 0){
                return getNodeByWeight(totalWeight,list);
            }

            BigDecimal totalWeightBigDecimal = BigDecimal.valueOf(totalWeight);
            for (MarsNode node : list) {
                BigDecimal weightBigDecimal = BigDecimal.valueOf(node.getNodeConfig().weight);
                BigDecimal nodeUserNumBigDecimal = nodeUserNumMap.get(node.getNodeConfig().getName());
                if(nodeUserNumBigDecimal == null){
                    return node;
                }

                BigDecimal userRatio = nodeUserNumBigDecimal.divide(allUserNumBigDecimal,4,BigDecimal.ROUND_HALF_UP);
                BigDecimal weightRatio = weightBigDecimal.divide(totalWeightBigDecimal,4,BigDecimal.ROUND_HALF_UP);
                if(userRatio.compareTo(weightRatio) < 0){
                    return node;
                }
            }
        }
        return getNodeByWeight(totalWeight,list);
    }

    private MarsNode getNodeByWeight(int totalWeight,List<MarsNode> list){
        int p = RandomUtils.randomInt(totalWeight);

        int sum = 0;
        for (MarsNode node : list) {
            sum += node.getNodeConfig().weight;
            if (p < sum) {
                return node;
            }
        }
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
