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
    //服务器ws地址
    private List<String> gameservers;
    //未充值服务器ws地址
    private List<String> poorGameservers;
    //资源地址
    private List<String> resourceurls;

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

    public List<String> getPoorGameservers() {
        return poorGameservers;
    }

    public void setPoorGameservers(List<String> poorGameservers) {
        this.poorGameservers = poorGameservers;
    }

    public List<String> getResourceurls() {
        return resourceurls;
    }

    public void setResourceurls(List<String> resourceurls) {
        this.resourceurls = resourceurls;
    }
}
