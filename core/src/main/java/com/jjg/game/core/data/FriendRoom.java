package com.jjg.game.core.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * 匹配房间，需要存库
 *
 * @author 2CL
 */
@Document(collection = "FriendRoom")
public class FriendRoom extends Room {
    @Id
    protected long id;
    // 房间过期时间
    protected long overdueTime;
    // 房间名
    protected String aliasName;
    // 是否开启自动续费
    protected boolean autoRenewal;
    // 预付金
    protected long predictCostGoldNum;
    // 房间状态 1. 运行中 2. 暂停中 3. 解散中
    protected int status;
    // 房间暂停时间，开启时需要置为0
    protected long pauseTime;
    // 屏蔽玩家
    protected List<Player> shieldPlayers;

    public long getOverdueTime() {
        return overdueTime;
    }

    public void setOverdueTime(long overdueTime) {
        this.overdueTime = overdueTime;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public boolean isAutoRenewal() {
        return autoRenewal;
    }

    public void setAutoRenewal(boolean autoRenewal) {
        this.autoRenewal = autoRenewal;
    }

    public long getPredictCostGoldNum() {
        return predictCostGoldNum;
    }

    public void setPredictCostGoldNum(long predictCostGoldNum) {
        this.predictCostGoldNum = predictCostGoldNum;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getPauseTime() {
        return pauseTime;
    }

    public void setPauseTime(long pauseTime) {
        this.pauseTime = pauseTime;
    }

    public List<Player> getShieldPlayers() {
        return shieldPlayers;
    }

    public void setShieldPlayers(List<Player> shieldPlayers) {
        this.shieldPlayers = shieldPlayers;
    }
}
