package com.jjg.game.ploy.games.highlowpoker.util;

import com.jjg.game.core.data.Card;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowChoose;
import com.jjg.game.ploy.games.highlowpoker.data.HighLowParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2026/4/1 11:35
 */
@Component
public class HighLowUtil {
    /**
     * 计算每种牌的赔率
     *
     * @param playerCards 玩家的牌
     * @return 赔率列表
     */
    public List<String> calculateAllChooseRate(List<Integer> playerCards, int currentIndex, BigDecimal returnRate) {
        List<String> chooseRate = new ArrayList<>(8);
        //红桃剩余数/剩余牌数
        //转换为card出现过的牌
        List<Card> appearCardList = List.of();
        if (currentIndex > 0) {
            appearCardList = playerCards.subList(0, currentIndex).stream().map(Card::new).toList();
        }
        //未出现牌的信息
        List<Card> remainCardList = playerCards.subList(currentIndex + 1, playerCards.size()).stream().map(Card::new).toList();
        //统计剩余牌的各种花色
        Map<Integer, Integer> remianSuitMap = new HashMap<>();
        for (Card card : remainCardList) {
            remianSuitMap.merge(card.getSuit(), 1, Integer::sum);
        }
        BigDecimal remainCount = BigDecimal.valueOf(remainCardList.size());
        //放入红桃数量
        int heart = remianSuitMap.getOrDefault(PokerCardUtils.EPokerSuit.HEART.getSuitId() - 1, 0);
        chooseRate.add(HighLowChoose.HEART.calculate(new HighLowParam(returnRate, remainCount, heart)));
        //放入方块数量
        int diamond = remianSuitMap.getOrDefault(PokerCardUtils.EPokerSuit.DIAMOND.getSuitId() - 1, 0);
        chooseRate.add(HighLowChoose.DIAMOND.calculate(new HighLowParam(returnRate, remainCount, diamond)));
        //放入红桃方块数量
        chooseRate.add(HighLowChoose.HEART_AND_DIAMOND.calculate(new HighLowParam(returnRate, remainCount, diamond + heart)));
        //放入黑桃数量
        int spades = remianSuitMap.getOrDefault(PokerCardUtils.EPokerSuit.SPADES.getSuitId() - 1, 0);
        chooseRate.add(HighLowChoose.SPADES.calculate(new HighLowParam(returnRate, remainCount, spades)));
        //放入梅花数量
        int clubs = remianSuitMap.getOrDefault(PokerCardUtils.EPokerSuit.CLUBS.getSuitId() - 1, 0);
        chooseRate.add(HighLowChoose.CLUBS.calculate(new HighLowParam(returnRate, remainCount, clubs)));
        //放入黑桃梅花数量
        chooseRate.add(HighLowChoose.SPADES_AND_CLUBS.calculate(new HighLowParam(returnRate, remainCount, spades + clubs)));

        //放入大于b代表已出现大于本牌点数的数量 x代表本牌的点数 a剩余牌数
        //4*(14-x)-b/a
        //获取当前牌的点数
        Card currentCard = new Card(playerCards.get(currentIndex));
        int currentPoint = currentCard.getRank() == 1 ? 14 : currentCard.getRank();
        //计算已出现大于当前牌的点数
        int greaterThanNum = 0;
        int lessThanNum = 0;
        if (!appearCardList.isEmpty()) {
            for (Card card : appearCardList) {
                int compare = card.compare(currentCard, false);
                if (compare > 0) {
                    greaterThanNum++;
                }
                if (compare < 0) {
                    lessThanNum++;
                }
            }
        }
        //放入小于数量
        chooseRate.add(HighLowChoose.LESS.calculate(new HighLowParam(returnRate, remainCount, currentPoint, lessThanNum)));
        //放入大于数量
        chooseRate.add(HighLowChoose.GREATER.calculate(new HighLowParam(returnRate, remainCount, currentPoint, greaterThanNum)));
        return chooseRate;
    }


}
