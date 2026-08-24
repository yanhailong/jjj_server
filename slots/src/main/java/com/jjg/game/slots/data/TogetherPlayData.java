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

}
