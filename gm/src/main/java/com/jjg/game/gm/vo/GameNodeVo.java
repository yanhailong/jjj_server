package com.jjg.game.gm.vo;

import com.jjg.game.common.net.NetAddress;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/25 17:04
 */
public class GameNodeVo {
    //节点类型
    private String type;
    //节点名称
    private String name;
    //节点tcp服务地址
    private NetAddress tcpAddress;
    //节点web服务地址
    private NetAddress httpAddress;
    //节点权重
    private int weight;
    //IP白名单
    private List<String> whiteIpList;
    //用户ID白名单
    private List<String> whiteIdList;
    //节点当前session数量
    private int sessionNum;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public List<String> getWhiteIpList() {
        return whiteIpList;
    }

    public void setWhiteIpList(List<String> whiteIpList) {
        this.whiteIpList = whiteIpList;
    }

    public List<String> getWhiteIdList() {
        return whiteIdList;
    }

    public void setWhiteIdList(List<String> whiteIdList) {
        this.whiteIdList = whiteIdList;
    }

    public int getSessionNum() {
        return sessionNum;
    }

    public void setSessionNum(int sessionNum) {
        this.sessionNum = sessionNum;
    }
}
