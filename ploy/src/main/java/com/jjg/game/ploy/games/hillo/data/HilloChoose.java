package com.jjg.game.ploy.games.hillo.data;

import com.jjg.game.core.data.Card;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

public enum HilloChoose {
    // 每个枚举对应一个前端投注区域：可用牌面、命中判断、命中牌数都封装在这里。
    GREATER_EQUAL(0, ">=", rank -> rank > 1 && rank < 13,
            (currentCard, nextCard) -> nextCard.compareAisMin(currentCard, false) >= 0,
            rank -> 14 - rank),
    LESS_EQUAL(1, "<=", rank -> rank > 1 && rank < 13,
            (currentCard, nextCard) -> nextCard.compareAisMin(currentCard, false) <= 0,
            rank -> rank),
    GREATER(2, ">", rank -> rank == 1,
            (currentCard, nextCard) -> nextCard.compareAisMin(currentCard, false) > 0,
            rank -> 12),
    EQUAL(3, "=", rank -> rank == 1 || rank == 13,
            (currentCard, nextCard) -> nextCard.compareAisMin(currentCard, false) == 0,
            rank -> 1),
    LESS(4, "<", rank -> rank == 13,
            (currentCard, nextCard) -> nextCard.compareAisMin(currentCard, false) < 0,
            rank -> 12);

    // HILLO 每次发牌都从完整 52 张牌中随机，概率计算不扣除已出现的牌。
    private static final BigDecimal FULL_DECK_COUNT = BigDecimal.valueOf(52);

    private final int chooseId;
    private final String chooseName;
    private final IntPredicate supportChecker;
    private final HilloCheck hilloCheck;
    private final IntUnaryOperator matchedRankCounter;

    HilloChoose(int chooseId, String chooseName, IntPredicate supportChecker, HilloCheck hilloCheck, IntUnaryOperator matchedRankCounter) {
        this.chooseId = chooseId;
        this.chooseName = chooseName;
        this.supportChecker = supportChecker;
        this.hilloCheck = hilloCheck;
        this.matchedRankCounter = matchedRankCounter;
    }

    public static HilloChoose getChoose(int chooseId) {
        for (HilloChoose choose : values()) {
            if (choose.chooseId == chooseId) {
                return choose;
            }
        }
        return null;
    }

    public static List<HilloChoose> getValidChoices(int currentCardId) {
        int rank = new Card(currentCardId).getRank();
        List<HilloChoose> result = new ArrayList<>(2);
        for (HilloChoose choose : values()) {
            if (choose.supportRank(rank)) {
                result.add(choose);
            }
        }
        return result;
    }

    public boolean supportCard(int currentCardId) {
        return supportRank(new Card(currentCardId).getRank());
    }

    public boolean supportRank(int currentRank) {
        return supportChecker.test(currentRank);
    }

    public boolean check(Card currentCard, Card nextCard) {
        return hilloCheck.check(currentCard, nextCard);
    }

    public int matchedCardCount(int currentRank) {
        return matchedRankCounter.applyAsInt(currentRank) * 4;
    }

    // 赔率 = 返奖率 / 命中概率，等价于 returnRate * 52 / 命中牌数。
    public String calculateOdds(BigDecimal returnRate, int currentRank) {
        int matchedCardCount = matchedCardCount(currentRank);
        if (matchedCardCount <= 0) {
            return "0";
        }
        return returnRate.multiply(FULL_DECK_COUNT)
                .divide(BigDecimal.valueOf(matchedCardCount), 2, RoundingMode.DOWN)
                .toPlainString();
    }

    // 胜率返回小数形式字符串，例如 0.9230 表示 92.30%。
    public String calculateWinRate(int currentRank) {
        int matchedCardCount = matchedCardCount(currentRank);
        if (matchedCardCount <= 0) {
            return "0";
        }
        return BigDecimal.valueOf(matchedCardCount)
                .divide(FULL_DECK_COUNT, 4, RoundingMode.DOWN)
                .toPlainString();
    }

    public int getChooseId() {
        return chooseId;
    }

    public String getChooseName() {
        return chooseName;
    }
}
