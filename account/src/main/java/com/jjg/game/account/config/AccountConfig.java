package com.jjg.game.account.config;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/9 16:04
 */
@Component
public class AccountConfig {
    private long playerBeginId;
    private List<String> gameservers;

    public long getPlayerBeginId() {
        return playerBeginId;
    }

    public void setPlayerBeginId(long playerBeginId) {
        this.playerBeginId = playerBeginId;
    }

    public List<String> getGameservers() {
        return gameservers;
    }

    public void setGameservers(List<String> gameservers) {
        this.gameservers = gameservers;
    }
}
