package com.jjg.game.ploy.data;

import com.jjg.game.core.data.Card;
import com.jjg.game.core.utils.PokerCardUtils;
import org.springframework.data.annotation.PersistenceCreator;

/**
 * @author 11
 * @date 2026/3/23
 */
public class PloyCard extends Card {
    //客户端展示的id
    private int clientCardId;

    public PloyCard(PokerCardUtils.EPokerSuit suit, int rank) {
        super(suit.getSuitId(), rank);
        this.clientCardId = PokerCardUtils.getCardId(suit, rank);
    }

    @PersistenceCreator
    public PloyCard(int suit, int rank, int clientCardId) {
        super(suit, rank);
        this.clientCardId = clientCardId;
    }

    public int getClientCardId() {
        return clientCardId;
    }

    public void setClientCardId(int clientCardId) {
        this.clientCardId = clientCardId;
    }
}
