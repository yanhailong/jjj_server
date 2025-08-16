package com.jjg.game.poker.game.blackjack.room.data;

import com.jjg.game.poker.game.blackjack.data.BlackJackDataHelper;
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

    /**
     * 结算延迟时间
     */
    private int settlementDelayTime;

    /**
     * 结算类型(0普通结算 1发牌结算)
     */
    private int settlementType;

    /**
     * 显示庄家的牌 (true 显示 false 不显示)
     */
    private boolean showDealer;

    public boolean isShowDealer() {
        return showDealer;
    }

    public void setShowDealer(boolean showDealer) {
        this.showDealer = showDealer;
    }

    public int getSettlementType() {
        return settlementType;
    }

    public void setSettlementType(int settlementType) {
        this.settlementType = settlementType;
    }

    public int getSettlementDelayTime() {
        return settlementDelayTime;
    }

    public void setSettlementDelayTime(int settlementDelayTime) {
        this.settlementDelayTime = settlementDelayTime;
    }

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
    public int getPoolId() {
        return BlackJackDataHelper.getPoolId(this);
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
        showDealer = true;
        settlementDelayTime = 0;
    }
}
