package com.jjg.game.account.dto;

/**
 * @author 11
 * @date 2025/10/11 17:10
 */
public class OAuthLoginDto extends LoginDto{
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
