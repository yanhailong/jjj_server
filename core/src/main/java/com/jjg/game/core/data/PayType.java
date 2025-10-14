package com.jjg.game.core.data;

/**
 * @author 11
 * @date 2025/10/13 16:17
 */
public enum PayType {
    GOOGLE(0),
    IOS(1),
    FACEBOOK(2),

    ;

    private int value;

    PayType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PayType valueOf(int value) {
        switch (value) {
            case 0 -> {
                return GOOGLE;
            }

            case 1 -> {
                return IOS;
            }

            case 2 -> {
                return FACEBOOK;
            }

            default -> {
                return null;
            }
        }
    }
}
