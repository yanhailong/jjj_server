package com.jjg.game.core.constant;

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

        //玩家起始id
        public static final long playerBeginId = 1000000;

        //最小房间id
        public static final int ROOM_ID_MIN = 100000;
        //最大房间id
        public static final int ROOM_ID_MAX = 999999;
    }


    public interface Redis {
        //游戏状态rediskey
        String GAME_STATUS_KEY = "gm:gamelistconfig";
    }
    //登录类型
    public class LoginType{
        //游客
        public static final int GUEST = 0;
        //手机
        public static final int PHONE = 1;
        //google邮箱
        public static final int GOOGLE_MAIL = 2;
    }

    public class ExcelGlobal{

    }
}
