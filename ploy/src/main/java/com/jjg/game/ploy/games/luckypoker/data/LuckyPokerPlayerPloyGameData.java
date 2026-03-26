package com.jjg.game.ploy.games.luckypoker.data;

import com.jjg.game.ploy.data.PlayerSinglePloyGameData;
import com.jjg.game.ploy.data.PloyCard;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 玩家在鸿运扑克中的数据
 * @author 11
 * @date 2026/3/19
 */
@Component
public class LuckyPokerPlayerPloyGameData extends PlayerSinglePloyGameData {
    //第一次发的牌
    private List<PloyCard> firstCardList;
    //第二次发的牌
    private List<PloyCard> secondCardList;
    //最终的手牌
    private List<PloyCard> finalCardList;

    public List<PloyCard> getFirstCardList() {
        return firstCardList;
    }

    public void setFirstCardList(List<PloyCard> firstCardList) {
        this.firstCardList = firstCardList;
    }

    public List<PloyCard> getSecondCardList() {
        return secondCardList;
    }

    public void setSecondCardList(List<PloyCard> secondCardList) {
        this.secondCardList = secondCardList;
    }

    public List<PloyCard> getFinalCardList() {
        return finalCardList;
    }

    public void setFinalCardList(List<PloyCard> finalCardList) {
        this.finalCardList = finalCardList;
    }
}
