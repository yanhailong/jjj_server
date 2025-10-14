package com.jjg.game.account.vo;

import java.util.List;

/**
 * @author 11
 * @date 2025/10/14 15:54
 */
public class ServerUrlVo {
    private List<String> gameServersUrls;
    private List<String> resourceUrls;

    public List<String> getGameServersUrls() {
        return gameServersUrls;
    }

    public void setGameServersUrls(List<String> gameServersUrls) {
        this.gameServersUrls = gameServersUrls;
    }

    public List<String> getResourceUrls() {
        return resourceUrls;
    }

    public void setResourceUrls(List<String> resourceUrls) {
        this.resourceUrls = resourceUrls;
    }
}
