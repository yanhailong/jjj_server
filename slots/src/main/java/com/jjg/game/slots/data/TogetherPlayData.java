package com.jjg.game.slots.data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 好友同玩信息
 */
public class TogetherPlayData {
    //本次进入期间已邀请的玩家，使用插入顺序支撑“我的邀请”分页
    private Set<Long> invitePlayerIds = new LinkedHashSet<>();
    //掉线时从实时集合中取出的本次进入净输赢
    private Long offlineWinGold;
    //全局技能提供的提成人数上限
    private int commissionUsers;
    //全局技能提供的中奖提成万分比
    private int winCommission;

    public Set<Long> getInvitePlayerIds() {
        if (invitePlayerIds == null) {
            invitePlayerIds = new LinkedHashSet<>();
        }
        return invitePlayerIds;
    }

    public void setInvitePlayerIds(Set<Long> invitePlayerIds) {
        this.invitePlayerIds = invitePlayerIds;
    }

    public Long getOfflineWinGold() {
        return offlineWinGold;
    }

    public void setOfflineWinGold(Long offlineWinGold) {
        this.offlineWinGold = offlineWinGold;
    }

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
