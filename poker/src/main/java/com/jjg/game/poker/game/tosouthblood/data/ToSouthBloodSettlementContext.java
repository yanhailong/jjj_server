package com.jjg.game.poker.game.tosouthblood.data;

import com.jjg.game.poker.game.common.data.PlayerSeatInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 南方前进-血战结算上下文
 * 封装结算所需的所有信息
 */
public class ToSouthBloodSettlementContext {
    private boolean isInstantWin;
    private final List<SettlementItem> settlementItems = new ArrayList<>();
    /**
     * 血战最终结算标志：true 时结算金额已在游戏过程中实时扣/加，
     * 结算阶段只需发通知、更新统计，不再重复计算扣款。
     */
    private boolean isBloodFinalSettlement;

    public ToSouthBloodSettlementContext() {
    }

    public boolean isInstantWin() {
        return isInstantWin;
    }

    public void setInstantWin(boolean instantWin) {
        isInstantWin = instantWin;
    }

    public List<SettlementItem> getSettlementItems() {
        return settlementItems;
    }

    public void addItem(SettlementItem item) {
        this.settlementItems.add(item);
    }
    
    public boolean isBloodFinalSettlement() {
        return isBloodFinalSettlement;
    }

    public void setBloodFinalSettlement(boolean bloodFinalSettlement) {
        isBloodFinalSettlement = bloodFinalSettlement;
    }

    public List<PlayerSeatInfo> getWinners() {
        List<PlayerSeatInfo> winners = new ArrayList<>();
        for (SettlementItem item : settlementItems) {
            if (item.isWinner) {
                winners.add(item.seatInfo);
            }
        }
        return winners;
    }

    public static class SettlementItem {
        public PlayerSeatInfo seatInfo;
        public boolean isWinner;
        public int instantWinType;
        public List<Integer> instantWinCards;

        public SettlementItem(PlayerSeatInfo seatInfo, boolean isWinner, int instantWinType, List<Integer> instantWinCards) {
            this.seatInfo = seatInfo;
            this.isWinner = isWinner;
            this.instantWinType = instantWinType;
            this.instantWinCards = instantWinCards;
        }
    }
}
