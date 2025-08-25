package com.jjg.game.common.config;

import com.jjg.game.common.net.NetAddress;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 集群节点配置信息
 *
 * @since 1.0
 */
@ConfigurationProperties(prefix = "cluster")
@Component
public class NodeConfig {
    //节点默认父目录
    protected String parentPath = "cluster";
    //节点类型
    protected String type;
    //节点名称
    protected String name;
    //节点tcp服务地址
    protected NetAddress tcpAddress;
    //节点web服务地址
    protected NetAddress httpAddress;
    //该节点支持的游戏主分类
    public int[] gameMajorTypes;
    //节点权重
    public int weight = 2;
    // 需要启动的游戏ID，仅本地调试生效，正式部署不起效
    public int[] needBootGameId;
    //IP白名单
    public String[] whiteIpList;
    //用户ID白名单
    public String[] whiteIdList;
    //是否暴露微服务
    public boolean showMicservice = true;
    //是否开启主节点选举
    public boolean masterSelect;

    public boolean open = true;
    //节点启动时是否只是和包含同种游戏类型的节点连接
    public boolean sameNodeJoin = true;
    //是否开启gm
    public boolean gm = false;

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NetAddress getTcpAddress() {
        return tcpAddress;
    }

    public void setTcpAddress(NetAddress tcpAddress) {
        this.tcpAddress = tcpAddress;
    }

    public NetAddress getHttpAddress() {
        return httpAddress;
    }

    public void setHttpAddress(NetAddress httpAddress) {
        this.httpAddress = httpAddress;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int[] getGameMajorTypes() {
        return gameMajorTypes;
    }

    public void setGameMajorTypes(int[] gameMajorTypes) {
        this.gameMajorTypes = gameMajorTypes;
    }

    public boolean inMajorType(int gameMajorType) {
        if (gameMajorTypes == null) {
            return false;
        }
        for (int v : gameMajorTypes) {
            if (v == gameMajorType) {
                return true;
            }
        }
        return false;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String[] getWhiteIpList() {
        return whiteIpList;
    }

    public void setWhiteIpList(String[] whiteIpList) {
        this.whiteIpList = whiteIpList;
    }

    public String[] getWhiteIdList() {
        return whiteIdList;
    }

    public void setWhiteIdList(String[] whiteIdList) {
        this.whiteIdList = whiteIdList;
    }

    public boolean isShowMicservice() {
        return showMicservice;
    }

    public void setShowMicservice(boolean showMicservice) {
        this.showMicservice = showMicservice;
    }

    public boolean isMasterSelect() {
        return masterSelect;
    }

    public void setMasterSelect(boolean masterSelect) {
        this.masterSelect = masterSelect;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isSameNodeJoin() {
        return sameNodeJoin;
    }

    public void setSameNodeJoin(boolean sameNodeJoin) {
        this.sameNodeJoin = sameNodeJoin;
    }

    public boolean isGm() {
        return gm;
    }

    public void setGm(boolean gm) {
        this.gm = gm;
    }

    public int[] getNeedBootGameId() {
        return needBootGameId;
    }

    public void setNeedBootGameId(int[] needBootGameId) {
        this.needBootGameId = needBootGameId;
    }
}
