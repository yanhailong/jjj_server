package com.jjg.game.table.redblackwar.data;

/**
 * @author lm
 * @date 2025/7/7 15:08
 */

public class Card {
    /**
     * 花色
     */
    public final int suit; // 0: ♠, 1: ♥, 2: ♣, 3: ♦
    /**
     * 点数
     */
    public final int rank; // 1~13

    public Card(int value) {
        this.suit = (value - 1) / 13;
        this.rank = (value - 1) % 13 + 1;
    }

    public int compare(Card card, boolean needSuit) {
        int diff = rank - card.rank;
        if (diff == 0 && needSuit) {
            diff = suit - card.suit;
        }
        return diff;
    }

    public int compare(Card card) {
        return compare(card, true);
    }

    public int getValue() {
        return suit * 13 + rank;
    }


    public int getSuit() {
        return suit;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        String[] suits = {"♠", "♥", "♣", "♦"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        return suits[suit] + ranks[rank - 1];
    }
}