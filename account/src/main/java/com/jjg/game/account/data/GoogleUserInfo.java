package com.jjg.game.account.data;

/**
 * @author 11
 * @date 2025/10/13 15:33
 */
public class GoogleUserInfo extends ChannelUserInfo{

    private String email;

    private boolean emailVerified;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
