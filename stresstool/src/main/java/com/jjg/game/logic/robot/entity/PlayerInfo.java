package com.jjg.game.logic.robot.entity;

import com.jjg.game.pbmsg.hall.GameListConfig;

import java.util.List;

/**
 * 游戏内的玩家player信息
 *
 * @author 2CL
 */
public class PlayerInfo {
    private long pid;
    private long diamond;
    private int vipLevel;
    public long playerId;
    public String nickName;
    public long gold;
    public List<GameListConfig> gameList;

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getDiamond() {
        return diamond;
    }

    public void setDiamond(long diamond) {
        this.diamond = diamond;
    }

    public int getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(int vipLevel) {
        this.vipLevel = vipLevel;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public long getGold() {
        return gold;
    }

    public void setGold(long gold) {
        this.gold = gold;
    }

    public List<GameListConfig> getGameList() {
        return gameList;
    }

    public void setGameList(List<GameListConfig> gameList) {
        this.gameList = gameList;
    }
}
