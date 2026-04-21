package com.jjg.game.poker.game.tosouthblood.room.data;

import com.jjg.game.poker.game.tosouthblood.util.ToSouthBloodCardType;

import java.util.List;

public class ToSouthBloodRoundRecord {
    public int seatId;
    public List<Integer> cards;
    public List<Integer> cardClientIds;
    public ToSouthBloodCardType cardType;

    public ToSouthBloodRoundRecord(int seatId, List<Integer> cards, List<Integer> cardClientIds, ToSouthBloodCardType cardType) {
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

    public ToSouthBloodCardType getCardType() {
        return cardType;
    }

    public void setCardType(ToSouthBloodCardType cardType) {
        this.cardType = cardType;
    }

    public List<Integer> getCardClientIds() {
        return cardClientIds;
    }

    public void setCardClientIds(List<Integer> cardClientIds) {
        this.cardClientIds = cardClientIds;
    }
}
