package com.jjg.game.table.game.redblackwar.data;

/**
 * @author lm
 * @date 2025/7/7 15:08
 */

public record Card(String points, int pointValue, String suit, int suitValue) {

    // A的值设为13，方便比较
    public int getValue() {
        return pointValue == 0 ? 13 : pointValue;
    }

    public int compare(Card card) {
        return compare(card, true);
    }

    public int compare(Card card, boolean needSuit) {
        int diff = getValue() - card.getValue();
        if (diff == 0 && needSuit) {
            diff = suitValue - card.suitValue;
        }
        return diff;
    }
}