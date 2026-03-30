package com.jjg.game.core.data;

import java.util.Arrays;

/**
 * 设备类型
 * @author 11
 * @date 2025/10/31 13:48
 */
public enum DeviceType {
    ANDROID(1),
    IOS(2),
    WEB(3)

    ;

    private int value;

    DeviceType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }


    public static DeviceType valueOf(int value) {
        return Arrays.stream(values()).filter(t -> t.getValue() == value).findFirst().orElse(null);
    }
}
