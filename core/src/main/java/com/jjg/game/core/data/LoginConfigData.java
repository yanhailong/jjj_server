package com.jjg.game.core.data;

/**
 * @author 11
 * @date 2026/1/26
 */
public class LoginConfigData {
    private int loginType;
    private boolean loginOpen;
    private boolean rewardOpen;

    public LoginConfigData() {
    }

    public LoginConfigData(int loginType, boolean loginOpen, boolean rewardOpen) {
        this.loginType = loginType;
        this.loginOpen = loginOpen;
        this.rewardOpen = rewardOpen;
    }

    public int getLoginType() {
        return loginType;
    }

    public void setLoginType(int loginType) {
        this.loginType = loginType;
    }

    public boolean isLoginOpen() {
        return loginOpen;
    }

    public void setLoginOpen(boolean loginOpen) {
        this.loginOpen = loginOpen;
    }

    public boolean isRewardOpen() {
        return rewardOpen;
    }

    public void setRewardOpen(boolean rewardOpen) {
        this.rewardOpen = rewardOpen;
    }
}
