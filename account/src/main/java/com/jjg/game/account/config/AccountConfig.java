package com.jjg.game.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/9 16:04
 */
@ConfigurationProperties(prefix = "jjgaccount")
@Component
public class AccountConfig {
    private String gameserver;
    private long playerBeginId = 1000000;

    public String getGameserver() {
        return gameserver;
    }

    public void setGameserver(String gameserver) {
        this.gameserver = gameserver;
    }

    public void setPlayerBeginId(long playerBeginId) {
        this.playerBeginId = playerBeginId;
    }

    public long getPlayerBeginId() {
        return playerBeginId;
    }
}
