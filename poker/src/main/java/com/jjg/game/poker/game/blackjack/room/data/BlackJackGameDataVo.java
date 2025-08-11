package com.jjg.game.poker.game.blackjack.room.data;

import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

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
     * 双倍下注玩家id
     */
    private final Set<Long> doubleBet = new HashSet<>();

    /**
     * 基础下注下注后不会变
     */
    private final Map<Long, Long> baseBet = new HashMap<>();

    public Set<Long> getDoubleBet() {
        return doubleBet;
    }

    public Map<Long, Long> getBaseBet() {
        return baseBet;
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

    @Override
    public void resetData(BasePokerGameController<? extends BasePokerGameDataVo> controller) {
        super.resetData(controller);
        baseBet.clear();
        doubleBet.clear();
        aceBuyPlayerIds.clear();
        dealerCards = null;
        canBuyACE = false;
    }
}
