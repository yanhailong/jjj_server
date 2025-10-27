package com.jjg.game.core.data;

/**
 * 渠道枚举
 * @author 11
 * @date 2025/10/13 11:37
 */
public enum ChannelType {
    GOOGLE(1),
    APPLE(2),
    FACEBOOK(3),

    ;

    private final int value;

    ChannelType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ChannelType valueOf(int value) {
        return switch (value) {
            case 0 -> GOOGLE;

            case 1 -> APPLE;

            case 2 -> FACEBOOK;

            default -> null;
        };
    }
}
