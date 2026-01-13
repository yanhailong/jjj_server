package com.jjg.game.core.constant;

public enum AccountStatus {
    NORMAL(0),
    BAN(1),
    DELETE(2),
    ;


    private int code;

    AccountStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static AccountStatus valueOf(int code) {
        for (AccountStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
