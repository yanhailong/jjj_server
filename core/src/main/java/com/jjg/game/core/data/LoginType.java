package com.jjg.game.core.data;

import java.util.Arrays;

/**
 * @author 11
 * @date 2025/10/13 16:38
 */
public enum LoginType {
    //游客
    GUEST(1),
    //谷歌
    GOOGLE(2),
    //苹果
    APPLE(3),
    //脸书
    FACEBOOK(4),
    //手机登录
    PHONE(5),
    //邮箱
    EMAIL(6),
    ;

    private int value;

    LoginType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static LoginType valueOf(int value) {
        return Arrays.stream(values()).filter(e -> e.getValue() == value).findFirst().orElse(null);
    }
}
