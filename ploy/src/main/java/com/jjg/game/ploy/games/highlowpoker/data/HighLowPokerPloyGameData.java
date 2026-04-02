package com.jjg.game.ploy.games.highlowpoker.data;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.ploy.data.PlayerSinglePloyGameData;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lm
 * @date 2026/4/1 09:47
 */
@Document
public class HighLowPokerPloyGameData extends PlayerSinglePloyGameData {
    //玩家当前的牌
    private List<Integer> card;
    //当前能兑换的金币
    private long currentCoin;
    //本局历史记录 key牌id，value赔率
    private List<Pair<Integer, String>> history;
    //整体历史记录
    private List<HighLowPokerHistory> totalHistories;
    //玩家当前牌的下标
    private int currentIndex;

    public List<Integer> getCard() {
        return card;
    }

    public void setCard(List<Integer> card) {
        this.card = card;
    }

    public long getCurrentCoin() {
        return currentCoin;
    }

    public void setCurrentCoin(long currentCoin) {
        this.currentCoin = currentCoin;
    }

    public List<Pair<Integer, String>> getHistory() {
        return history;
    }

    public void setHistory(List<Pair<Integer, String>> history) {
        this.history = history;
    }

    public List<HighLowPokerHistory> getTotalHistories() {
        return totalHistories;
    }

    public void setTotalHistories(List<HighLowPokerHistory> totalHistories) {
        this.totalHistories = totalHistories;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public void addHistory(Pair<Integer, String> history) {
        if (this.history == null) {
            this.history = new ArrayList<>();
        }
        this.history.add(history);
    }

    public void addTotalHistory(HighLowPokerHistory totalHistory) {
        if (this.totalHistories == null) {
            this.totalHistories = new ArrayList<>();
        }
        this.totalHistories.add(totalHistory);
        int removeNum = this.totalHistories.size() - HighLowPokerConstant.Common.MAX_RECORD_NUM;
        if (removeNum > 0) {
            for (int i = 0; i < removeNum; i++) {
                this.totalHistories.removeFirst();
            }
        }
    }
}
