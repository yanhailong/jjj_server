package com.jjg.game.core.data;

import java.util.Map;

public class SlotsBillInfo {
    private long totalFlowing;
    private long totalIncome;
    // 参与玩家收益 玩家ID + 收益数量
    private Map<Long, Long> partInPlayerIncome;
    // 玩家押分 玩家ID + 玩家押分
    private Map<Long, Long> partInPlayerBet;

    public long getTotalFlowing() {
        return totalFlowing;
    }

    public void setTotalFlowing(long totalFlowing) {
        this.totalFlowing = totalFlowing;
    }

    public long getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(long totalIncome) {
        this.totalIncome = totalIncome;
    }

    public Map<Long, Long> getPartInPlayerIncome() {
        return partInPlayerIncome;
    }

    public void setPartInPlayerIncome(Map<Long, Long> partInPlayerIncome) {
        this.partInPlayerIncome = partInPlayerIncome;
    }

    public Map<Long, Long> getPartInPlayerBet() {
        return partInPlayerBet;
    }

    public void setPartInPlayerBet(Map<Long, Long> partInPlayerBet) {
        this.partInPlayerBet = partInPlayerBet;
    }
}
