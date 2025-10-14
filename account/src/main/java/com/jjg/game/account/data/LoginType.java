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
    PHONE(5)

    ;

    private int value;

    LoginType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LoginType valueOf(int value) {
        switch (value) {
            case 1 -> {
                return GUEST;
            }
            case 2 -> {
                return GOOGLE;
            }
            case 3 -> {
                return APPLE;
            }
            case 4 -> {
                return FACEBOOK;
            }
            case 5 -> {
                return PHONE;
            }
            default -> {
                return null;
            }
        }
    }
}
