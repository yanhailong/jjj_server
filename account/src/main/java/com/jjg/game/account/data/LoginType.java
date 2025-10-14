package com.jjg.game.account.data;

/**
 * @author 11
 * @date 2025/10/13 16:38
 */
public enum LoginType {
    GUEST(1),
    GOOGLE(2),
    APPLE(3),
    FACEBOOK(4),

    ;

    private int value;

    LoginType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
