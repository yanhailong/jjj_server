package com.jjg.game.poker.game.tosouth.room.data;

import com.jjg.game.poker.game.tosouth.util.ToSouthCardType;
import java.util.List;

public class ToSouthRoundRecord {
    public int seatId;
    public List<Integer> cards;
    public ToSouthCardType cardType;

    public ToSouthRoundRecord(int seatId, List<Integer> cards, ToSouthCardType cardType) {
        this.seatId = seatId;
        this.cards = cards;
        this.cardType = cardType;
    }
}
