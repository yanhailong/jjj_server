package com.jjg.game.poker.game.texas.util;

import com.jjg.game.core.data.Card;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/30 15:54
 */
public class PlayerHand {
    private final long playerId;
    private final List<Card> holeCards;

    public PlayerHand(long playerId, List<Card> holeCards) {
        this.playerId = playerId;
        this.holeCards = holeCards;
    }

    public long getPlayerId() {
        return playerId;
    }

    public List<Card> getHoleCards() {
        return holeCards;
    }
}
