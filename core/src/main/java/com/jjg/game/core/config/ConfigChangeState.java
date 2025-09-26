package com.jjg.game.core.config;


/**
 * 配置变化状态
 */
public enum ConfigChangeState {

    ADD("新增", 0),

    UPDATE("更新", 1),

    DELETE("删除", 2),
    ;

    private final String str;

    private final int code;

    ConfigChangeState(String str, int code) {
        this.str = str;
        this.code = code;
    }

    public String getStr() {
        return str;
    }

    public int getCode() {
        return code;
    }
}
