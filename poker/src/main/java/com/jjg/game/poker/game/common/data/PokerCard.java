package com.jjg.game.poker.game.common.data;

import com.jjg.game.core.data.Card;

/**
 * @author lm
 * @date 2025/8/2 14:40
 */
public class PokerCard extends Card {
    /**
     * PokerPool表的id
     */
    private final int pokerPoolId;

    /**
     * 前端牌id
     */
    private final int clientId;
    /**
     * @param pokerPoolId PokerPool表的id
     * @param suit        花色值
     * @param rank        点数值
     */
    public PokerCard(int pokerPoolId, int suit, int rank, int clientId) {
        super(suit, rank);
        this.pokerPoolId = pokerPoolId;
        this.clientId = clientId;
    }

    public int getClientId() {
        return clientId;
    }

    public int getPokerPoolId() {
        return pokerPoolId;
    }

}
