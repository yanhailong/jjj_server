package com.jjg.game.poker.game.douxian.util;

import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.douxian.constant.DouXianZone;

import java.util.List;

/**
 * 单个区域的成牌结果：牌型 + 主体点数 + 组成牌 + 灵力值（含回合倍率）
 */
public class DouXianHandResult {

    private final DouXianZone zone;
    private final IDouXianHandType handType;
    /**
     * 主体牌型中最大那张牌的点数（A参与低位顺子时按1点计）
     */
    private final int dominantRank;
    private final List<Card> cards;
    private final long aetherValue;

    public DouXianHandResult(DouXianZone zone, IDouXianHandType handType, int dominantRank,
                              List<Card> cards, long aetherValue) {
        this.zone = zone;
        this.handType = handType;
        this.dominantRank = dominantRank;
        this.cards = cards;
        this.aetherValue = aetherValue;
    }

    public DouXianZone getZone() {
        return zone;
    }

    public IDouXianHandType getHandType() {
        return handType;
    }

    public int getDominantRank() {
        return dominantRank;
    }

    public List<Card> getCards() {
        return cards;
    }

    public long getAetherValue() {
        return aetherValue;
    }
}
