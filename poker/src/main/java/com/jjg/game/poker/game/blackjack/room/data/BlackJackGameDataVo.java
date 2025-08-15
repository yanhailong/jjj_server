package com.jjg.game.poker.game.blackjack.room.data;

import com.jjg.game.poker.game.blackjack.message.resp.NotifyBlackJackSettlementInfo;
import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;

/**
 * @author lm
 * @date 2025/7/28 14:04
 */
public class BlackJackGameDataVo extends BasePokerGameDataVo {
    /**
     * 庄家的牌信息
     */
    private List<Integer> dealerCards;

    /**
     * 本轮是否能购买ACE
     */
    private boolean canBuyACE;

    /**
     * ACE玩家id
     */
    private final Set<Long> aceBuyPlayerIds = new HashSet<>();

    /**
     * 基础下注下注后不会变
     * 玩家id->牌索引->总下注 不包含购买ACE
     */
    private final Map<Long, Map<Integer, Long>> allBetInfo = new HashMap<>();

    /**
     * 结算信息
     */
    private NotifyBlackJackSettlementInfo settlementInfo;

    /**
     * 购买ACE结束时间
     */

    private long aceBuyEndTime;

    public long getAceBuyEndTime() {
        return aceBuyEndTime;
    }

    public void setAceBuyEndTime(long aceBuyEndTime) {
        this.aceBuyEndTime = aceBuyEndTime;
    }

    public NotifyBlackJackSettlementInfo getSettlementInfo() {
        return settlementInfo;
    }

    public void setSettlementInfo(NotifyBlackJackSettlementInfo settlementInfo) {
        this.settlementInfo = settlementInfo;
    }

    public Map<Long, Map<Integer, Long>> getAllBetInfo() {
        return allBetInfo;
    }

    public Set<Long> getAceBuyPlayerIds() {
        return aceBuyPlayerIds;
    }

    public boolean isCanBuyACE() {
        return canBuyACE;
    }

    public void setCanBuyACE(boolean canBuyACE) {
        this.canBuyACE = canBuyACE;
    }

    public BlackJackGameDataVo(Room_ChessCfg roomCfg) {
        super(roomCfg);
    }

    public List<Integer> getDealerCards() {
        return dealerCards;
    }

    public void setDealerCards(List<Integer> dealerCards) {
        this.dealerCards = dealerCards;
    }

    public boolean canStartGame() {
        int seatDownNum = getSeatDownNum() + 1;
        return seatDownNum >= getRoomCfg().getMinPlayer() && seatDownNum <= getRoomCfg().getMaxPlayer();
    }

    @Override
    public void resetData(BasePokerGameController<? extends BasePokerGameDataVo> controller) {
        super.resetData(controller);
        aceBuyEndTime = 0;
        setIndex(0);
        settlementInfo = null;
        allBetInfo.clear();
        aceBuyPlayerIds.clear();
        dealerCards = null;
        canBuyACE = false;
    }
}
