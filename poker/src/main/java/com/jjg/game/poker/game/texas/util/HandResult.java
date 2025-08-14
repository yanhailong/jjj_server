package com.jjg.game.poker.game.texas.util;

import com.jjg.game.core.data.Card;

import java.util.List;

/**
 * @author lm
 * @date 2025/7/30 15:42
 */
public class HandResult implements Comparable<HandResult> {
    private final HandRank handRank;
    private final List<Integer> kickers; // 用于打平时比较

    private final List<Card> bestCards;

    public HandResult(HandRank handRank, List<Integer> kickers, List<Card> bestCards) {
        this.handRank = handRank;
        this.kickers = kickers;
        this.bestCards = bestCards;
    }

    public HandRank getHandRank() {
        return handRank;
    }

    public List<Card> getBestCards() {
        return bestCards;
    }

    @Override
    public int compareTo(HandResult o) {
        if (this.handRank.rank != o.handRank.rank) {
            return Integer.compare(this.handRank.rank, o.handRank.rank);
        }
        //顺子A2345是最小的
        if (handRank == HandRank.STRAIGHT || handRank == HandRank.STRAIGHT_FLUSH) {
            boolean isMin = isA2345(kickers);
            boolean oisMin = isA2345(o.kickers);
            if (isMin && !oisMin) {
                return -1;
            }
            if (!isMin && oisMin) {
                return 1;
            }
            if (isMin) {
                return 0;
            }
        }
        for (int i = 0; i < this.kickers.size(); i++) {
            int cmp = Integer.compare(this.kickers.get(i), o.kickers.get(i));
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    public boolean isA2345(List<Integer> kickers) {
        return kickers.getFirst().equals(14) && kickers.contains(2);
    }
}