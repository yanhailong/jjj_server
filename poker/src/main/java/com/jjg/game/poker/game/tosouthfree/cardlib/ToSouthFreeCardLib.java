package com.jjg.game.poker.game.tosouthfree.cardlib;

import com.jjg.game.poker.game.common.cardlib.CardLibEntry;

import java.util.List;

/**
 * 南方前进牌库条目
 * 每场模拟生成两条记录:
 * - multiplier < 0 (输牌): 总输最多的座位 → playerCards，其余3人 → robotCards
 * - multiplier > 0 (赢牌): 总赢最多的座位 → playerCards，其余3人 → robotCards
 *
 * playerCards 始终是"特殊玩家"的牌(输/赢最多的那个座位的牌)
 * 抽中后发给触发权重修改的真人玩家
 */
public class ToSouthFreeCardLib implements CardLibEntry {

    /** 玩家位的输赢倍数(有符号: 负数=输, 正数=赢, 单位=底注倍数) */
    private long multiplier;

    /** 玩家手牌 (13张 pokerPoolId) */
    private List<Integer> playerCards;

    /** 3个机器人手牌 (各13张 pokerPoolId) */
    private List<List<Integer>> robotCards;

    public ToSouthFreeCardLib() {
    }

    public ToSouthFreeCardLib(long multiplier, List<Integer> playerCards, List<List<Integer>> robotCards) {
        this.multiplier = multiplier;
        this.playerCards = playerCards;
        this.robotCards = robotCards;
    }

    public long getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(long multiplier) {
        this.multiplier = multiplier;
    }

    public List<Integer> getPlayerCards() {
        return playerCards;
    }

    public void setPlayerCards(List<Integer> playerCards) {
        this.playerCards = playerCards;
    }

    public List<List<Integer>> getRobotCards() {
        return robotCards;
    }

    public void setRobotCards(List<List<Integer>> robotCards) {
        this.robotCards = robotCards;
    }
}
