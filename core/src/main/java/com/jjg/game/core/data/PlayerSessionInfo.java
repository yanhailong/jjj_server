package com.jjg.game.core.data;

/**
 * 玩家节点信息
 * @author 11
 * @date 2025/5/26 16:58
 */
public class PlayerSessionInfo {
    private long playerId;
    private String sessionId;
    //当前用户所在网关节点
    private String nodeName;
    //当前玩家的游戏类型
    private int gameType;
    //当前玩家的游戏场次id
    private int wareId;
    //当前用户所在节点
    private String currentNode;
    //最近活跃时间（指最近切换节点）
    private long lastActiveTime;


    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getWareId() {
        return wareId;
    }

    public void setWareId(int wareId) {
        this.wareId = wareId;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
}
