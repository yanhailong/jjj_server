package com.vegasnight.game.core.constant;

/**
 * 游戏常量
 * @author 11
 * @date 2025/5/26 11:28
 */
public class GameConstant {
    public class Common{
        //redisLock重试次数
        public static final int REDIS_LOCK_TRY_COUNT = 5;
        public static final int REDIS_TRANSACTION_TRY_COUNT = 7;

        public static final String ENCODING = "UTF-8";
    }
}
