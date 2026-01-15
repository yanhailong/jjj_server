package com.jjg.game.account.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/9 16:04
 */
@Component
public class AccountConfig {
    private long playerBeginId;
    //服务器ws地址
    private List<String> gameservers;

    private Map<String, List<String>> flags;
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

    public Map<String, List<String>> getFlags() {
        return flags;
    }

    public void setFlags(Map<String, List<String>> flags) {
        this.flags = flags;
    }

    public List<String> getResourceurls() {
        return resourceurls;
    }

    public void setResourceurls(List<String> resourceurls) {
        this.resourceurls = resourceurls;
    }
}
