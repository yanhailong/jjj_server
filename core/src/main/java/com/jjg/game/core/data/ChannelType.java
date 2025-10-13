package com.jjg.game.core.data;

/**
 * 渠道枚举
 * @author 11
 * @date 2025/10/13 11:37
 */
public enum ChannelType {
    GOOGLE(0),
    IOS(1),
    FACEBOOK(2),

    ;

    private int value;

    ChannelType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ChannelType valueOf(int value) {
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
