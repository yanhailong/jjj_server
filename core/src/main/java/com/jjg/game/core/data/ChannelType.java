package com.jjg.game.core.data;

import java.util.Arrays;

/**
 * 渠道枚举
 *
 * @author 11
 * @date 2025/10/13 11:37
 */
public enum ChannelType {
    GOOGLE(1),
    APPLE(2),
    ;

    private final int value;

    ChannelType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ChannelType valueOf(int value) {
        return valueOf(value, null);
    }

    public static ChannelType valueOf(int value, ChannelType defaultType) {
        return Arrays.stream(values()).filter(t -> t.getValue() == value).findFirst().orElse(defaultType);
    }
}
