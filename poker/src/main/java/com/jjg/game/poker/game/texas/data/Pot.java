package com.jjg.game.poker.game.texas.data;

import java.util.HashSet;
import java.util.Set;

/**
 * 奖池
 * @author lm
 * @date 2025/7/29 14:36
 */
public class Pot {
    private long amount; // 当前池中总筹码
    private final Set<Long> eligiblePlayers; // 有资格争夺这个池的玩家

    public Pot() {
        this.amount = 0;
        this.eligiblePlayers = new HashSet<>();
    }

    public void addChips(long chips) {
        this.amount += chips;
    }

    public void addEligiblePlayer(long playerId) {
        eligiblePlayers.add(playerId);
    }

    public long getAmount() {
        return amount;
    }

    public Set<Long> getEligiblePlayers() {
        return eligiblePlayers;
    }
}
