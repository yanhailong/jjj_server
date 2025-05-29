package com.vegasnight.game.account.vo;

/**
 * @author 11
 * @date 2025/5/24 17:31
 */
public class LoginVo {
    private String token;
    private String gameserver;


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getGameserver() {
        return gameserver;
    }

    public void setGameserver(String gameserver) {
        this.gameserver = gameserver;
    }
}
