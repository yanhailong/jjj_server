package com.jjg.game.slots.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

/**
 * 记录玩家玩过哪些slots游戏
 * @author 11
 * @date 2025/7/11 10:27
 */
@Document
public class PlayerHistorySlots {
    @Id
    private long playerId;
    private Set<Integer> slots;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public Set<Integer> getSlots() {
        return slots;
    }

    public void setSlots(Set<Integer> slots) {
        this.slots = slots;
    }
}
