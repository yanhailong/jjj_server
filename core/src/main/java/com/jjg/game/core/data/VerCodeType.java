package com.jjg.game.core.data;

/**
 * 验证码类型
 * @author 11
 * @date 2025/10/17 16:05
 */
public enum VerCodeType {
    //绑定手机号
    SMS_BIND_PHONE(0),
    //绑定邮箱
    MAIL_BIND_MAIL(1),
    //短信登录
    SMS_LOGIN(2);


    private int value;

    VerCodeType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static VerCodeType getType(int value) {
        switch (value) {
            case 0 -> {
                return SMS_BIND_PHONE;
            }
            case 1 -> {
                return MAIL_BIND_MAIL;
            }
            case 2 -> {
                return SMS_LOGIN;
            }
            default -> {return null;}
        }
    }
}
