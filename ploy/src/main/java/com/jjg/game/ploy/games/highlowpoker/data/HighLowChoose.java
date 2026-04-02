package com.jjg.game.ploy.games.highlowpoker.data;

import com.jjg.game.core.data.Card;
import com.jjg.game.core.utils.PokerCardUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

/**
 * @author lm
 * @date 2026/4/1 14:30
 */
public enum HighLowChoose {
    /**
     * 红桃
     */
    HEART(0, ((old, now) -> now.getSuit() == PokerCardUtils.EPokerSuit.HEART.getSuitId() - 1),
            HighLowChoose::calculateOdds),
    /**
     * 方块
     */
    DIAMOND(1, ((old, now) -> now.getSuit() == PokerCardUtils.EPokerSuit.DIAMOND.getSuitId() - 1), HighLowChoose::calculateOdds),
    /**
     * 红桃和方块
     */
    HEART_AND_DIAMOND(2, ((old, now) -> now.getSuit() == PokerCardUtils.EPokerSuit.DIAMOND.getSuitId() - 1
            || now.getSuit() == PokerCardUtils.EPokerSuit.HEART.getSuitId() - 1), HighLowChoose::calculateOdds),
    /**
     * 黑桃
     */
    SPADES(3, ((old, now) -> now.getSuit() == PokerCardUtils.EPokerSuit.SPADES.getSuitId() - 1), HighLowChoose::calculateOdds),
    /**
     * 梅花
     */
    CLUBS(4, ((old, now) -> now.getSuit() == PokerCardUtils.EPokerSuit.CLUBS.getSuitId() - 1), HighLowChoose::calculateOdds),
    /**
     * 黑桃和梅花
     */
    SPADES_AND_CLUBS(5, ((old, now) -> now.getSuit() == PokerCardUtils.EPokerSuit.SPADES.getSuitId() - 1
            || now.getSuit() == PokerCardUtils.EPokerSuit.CLUBS.getSuitId() - 1), HighLowChoose::calculateOdds),
    /**
     * 小于
     */
    LESS(6, ((old, now) -> now.compare(old, false) < 0), (param) -> {
        BigDecimal pro = BigDecimal.valueOf(4L * (param.currentPoint() - 2) - param.num()).divide(param.remainCount(), 4, RoundingMode.DOWN);
        if (pro.compareTo(BigDecimal.ZERO) > 0) {
            return param.returnRate().divide(pro, 2, RoundingMode.DOWN)
                    .toPlainString();
        } else {
            return "0";
        }
    }),
    /**
     * 大于
     */
    GREATER(7, ((old, now) -> now.compare(old, false) > 0), (param) -> {
        //放入大于数量
        BigDecimal pro = BigDecimal.valueOf(4L * (14 - param.currentPoint()) - param.num()).divide(param.remainCount(), 4, RoundingMode.DOWN);
        if (pro.compareTo(BigDecimal.ZERO) > 0) {
            return param.returnRate().divide(pro, 2, RoundingMode.DOWN)
                    .toPlainString();
        } else {
            return "0";
        }
    });

    private final int index;
    private final HighLowCheck highLowCheck;
    private final Function<HighLowParam, String> calculate;

    HighLowChoose(int index, HighLowCheck highLowCheck, Function<HighLowParam, String> calculate) {
        this.index = index;
        this.highLowCheck = highLowCheck;
        this.calculate = calculate;
    }

    public static HighLowChoose getChoose(int index) {
        for (HighLowChoose highLowChoose : HighLowChoose.values()) {
            if (highLowChoose.getIndex() == index) {
                return highLowChoose;
            }
        }
        return null;
    }

    private static String calculateOdds(HighLowParam highLowParam) {
        if (highLowParam.num() == 0) {
            return "0";
        }
        return highLowParam.returnRate()
                .divide(BigDecimal.valueOf(highLowParam.num()).divide(highLowParam.remainCount(), 4, RoundingMode.DOWN), 2, RoundingMode.DOWN)
                .toPlainString();
    }

    public int getIndex() {
        return index;
    }

    public boolean check(Card old, Card now) {
        return highLowCheck.check(old, now);
    }

    public String calculate(HighLowParam highLowParam) {
        return calculate.apply(highLowParam);
    }
}
