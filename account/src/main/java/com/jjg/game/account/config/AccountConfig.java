package com.jjg.game.account.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/9 16:04
 */
@ConfigurationProperties(prefix = "vegasaccount")
@Component
public class AccountConfig {
    private String nodename;
    private String gameserver;

    public String getNodename() {
        return nodename;
    }

    public void setNodename(String nodename) {
        this.nodename = nodename;
    }

    public String getGameserver() {
        return gameserver;
    }

    public void setGameserver(String gameserver) {
        this.gameserver = gameserver;
    }
}
