package com.jjg.game.poker.game.blackjack.room.data;

import com.jjg.game.poker.game.common.BasePokerGameController;
import com.jjg.game.poker.game.common.BasePokerGameDataVo;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.List;
import java.util.Map;

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
     * ACE购买列表
     */
    private List<Long> aceBuyPlayerIds;

    public List<Long> getAceBuyPlayerIds() {
        return aceBuyPlayerIds;
    }

    public void setAceBuyPlayerIds(List<Long> aceBuyPlayerIds) {
        this.aceBuyPlayerIds = aceBuyPlayerIds;
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
        dealerCards = null;
        canBuyACE = false;
    }
}
