package com.jjg.game.common.utils;

import cn.hutool.core.lang.Snowflake;

/**
 * @author lm
 * @date 2025/8/7 10:07
 */
public enum IDGenUtil {
    TEXAS(1);
    private final Snowflake snowflake;
    IDGenUtil(int workId) {
        this.snowflake = new Snowflake(workId);
    }
    public long getNextId() {
        return snowflake.nextId();
    }
}
