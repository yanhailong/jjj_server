package com.jjg.game.slots.data;

/**
 * @author 11
 * @date 2025/8/5 14:11
 */
public class SlotsPlayerGameDataDTO {
    //游戏类型
    protected int gameType;
    //场次配置id
    protected int roomCfgId;
    //当前所处状态(美元快递) 0.正常  1.二选一  2.正在免费旋转
    protected int status;
    //原始押注值
    protected long lastStake;

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getRoomCfgId() {
        return roomCfgId;
    }

    public void setRoomCfgId(int roomCfgId) {
        this.roomCfgId = roomCfgId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getLastStake() {
        return lastStake;
    }

    public void setLastStake(long lastStake) {
        this.lastStake = lastStake;
    }
}
