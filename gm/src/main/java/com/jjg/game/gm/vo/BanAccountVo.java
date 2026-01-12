package com.jjg.game.gm.vo;

public class BanAccountVo {
    //玩家id
    long playerId;
    //期望修改后的状态  0.正常  1.封禁
    private int changeStatus;
    //是否成功
    private boolean success;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public int getChangeStatus() {
        return changeStatus;
    }

    public void setChangeStatus(int changeStatus) {
        this.changeStatus = changeStatus;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
