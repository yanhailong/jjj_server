package com.jjg.game.poker.game.tosouth.room.data;

import com.jjg.game.poker.game.tosouth.util.ToSouthCardType;
import java.util.List;

public class ToSouthRoundRecord {
    public int seatId;
    public List<Integer> cards;
    public List<Integer> cardClientIds;
    public ToSouthCardType cardType;

    public ToSouthRoundRecord(int seatId, List<Integer> cards, List<Integer> cardClientIds, ToSouthCardType cardType) {
        this.seatId = seatId;
        this.cards = cards;
        this.cardClientIds = cardClientIds;
        this.cardType = cardType;
    }

    public int getSeatId() {
        return seatId;
    }

    public void setSeatId(int seatId) {
        this.seatId = seatId;
    }

    public List<Integer> getCards() {
        return cards;
    }

    public void setCards(List<Integer> cards) {
        this.cards = cards;
    }

    public ToSouthCardType getCardType() {
        return cardType;
    }

    public void setCardType(ToSouthCardType cardType) {
        this.cardType = cardType;
    }

    public List<Integer> getCardClientIds() {
        return cardClientIds;
    }

    public void setCardClientIds(List<Integer> cardClientIds) {
        this.cardClientIds = cardClientIds;
    }
}
