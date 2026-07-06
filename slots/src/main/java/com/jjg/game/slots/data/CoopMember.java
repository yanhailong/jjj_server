package com.jjg.game.slots.data;

import com.jjg.game.core.data.PlayerController;

/**
 * 协作房间成员 (内存态)。
 *
 * @author 11
 * @date 2026/7/6
 */
public class CoopMember {
    private final long playerId;
    //座位序 (进房顺序, 平分余数按此序分配)
    private final int seat;
    private String name;
    private int headImgId;
    private int headFrameId;
    //协助者准备态 (房主恒为 true)
    private boolean ready;
    //在线 (断线标记, RUNNING 中断线不移除)
    private boolean online = true;
    //个人 Spin 份额 (血条上限, 开始时平分)
    private int spinQuota;
    //已消耗 Spin (已扣血量)
    private int spinUsed;
    //会话引用 (广播用; 断线置空)
    private transient PlayerController playerController;

    public CoopMember(long playerId, int seat) {
        this.playerId = playerId;
        this.seat = seat;
    }

    public long getPlayerId() {
        return playerId;
    }

    public int getSeat() {
        return seat;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHeadImgId() {
        return headImgId;
    }

    public void setHeadImgId(int headImgId) {
        this.headImgId = headImgId;
    }

    public int getHeadFrameId() {
        return headFrameId;
    }

    public void setHeadFrameId(int headFrameId) {
        this.headFrameId = headFrameId;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public int getSpinQuota() {
        return spinQuota;
    }

    public void setSpinQuota(int spinQuota) {
        this.spinQuota = spinQuota;
    }

    public int getSpinUsed() {
        return spinUsed;
    }

    public void setSpinUsed(int spinUsed) {
        this.spinUsed = spinUsed;
    }

    public int hpLeft() {
        return Math.max(0, spinQuota - spinUsed);
    }

    public PlayerController getPlayerController() {
        return playerController;
    }

    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }
}
