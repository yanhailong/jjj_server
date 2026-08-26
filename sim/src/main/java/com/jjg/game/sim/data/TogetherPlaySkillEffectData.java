package com.jjg.game.sim.data;

/**
 * 好友同玩使用的全局技能效果快照。
 */
public class TogetherPlaySkillEffectData {
    private int commissionUsers;
    private int winCommission;

    public int getCommissionUsers() {
        return commissionUsers;
    }

    public void setCommissionUsers(int commissionUsers) {
        this.commissionUsers = commissionUsers;
    }

    public int getWinCommission() {
        return winCommission;
    }

    public void setWinCommission(int winCommission) {
        this.winCommission = winCommission;
    }
}
