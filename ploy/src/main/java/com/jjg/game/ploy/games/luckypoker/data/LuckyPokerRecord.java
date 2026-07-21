package com.jjg.game.ploy.games.luckypoker.data;

import com.jjg.game.ploy.data.PloyRecord;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * @author 11
 * @date 2026/3/24
 */
@Document
public class LuckyPokerRecord extends PloyRecord {
    //最终手牌
    private List<Integer> finalCardIds;
    //牌型
    private PokerRank pokerRank;
    //倍数
    private int times;
    //下注金额
    private long betAmount;
    //赢得金额(税后)
    private long winAmount;

    public List<Integer> getFinalCardIds() {
        return finalCardIds;
    }

    public void setFinalCardIds(List<Integer> finalCardIds) {
        this.finalCardIds = finalCardIds;
    }

    public PokerRank getPokerRank() {
        return pokerRank;
    }

    public void setPokerRank(PokerRank pokerRank) {
        this.pokerRank = pokerRank;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public long getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(long betAmount) {
        this.betAmount = betAmount;
    }

    public long getWinAmount() {
        return winAmount;
    }

    public void setWinAmount(long winAmount) {
        this.winAmount = winAmount;
    }
}
