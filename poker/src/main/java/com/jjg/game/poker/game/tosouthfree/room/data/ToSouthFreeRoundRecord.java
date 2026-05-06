package com.jjg.game.poker.game.tosouthfree.room.data;

import com.jjg.game.poker.game.tosouthfree.util.ToSouthFreeCardType;

import java.util.List;

public class ToSouthFreeRoundRecord {
    public int seatId;
    public List<Integer> cards;
    public List<Integer> cardClientIds;
    public ToSouthFreeCardType cardType;

    public ToSouthFreeRoundRecord(int seatId, List<Integer> cards, List<Integer> cardClientIds, ToSouthFreeCardType cardType) {
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

    public ToSouthFreeCardType getCardType() {
        return cardType;
    }

    public void setCardType(ToSouthFreeCardType cardType) {
        this.cardType = cardType;
    }

    public List<Integer> getCardClientIds() {
        return cardClientIds;
    }

    public void setCardClientIds(List<Integer> cardClientIds) {
        this.cardClientIds = cardClientIds;
    }
}
