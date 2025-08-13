package com.jjg.game.core.constant;

import com.jjg.game.common.utils.CommonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏常量
 *
 * @author 11
 * @date 2025/5/26 11:28
 */
public class GameConstant {
    //游戏主分类 -> 游戏列表
    public static final Map<Integer, List<EGameType>> MAJOR_TYPE_ID_SET = new HashMap<>();

    static {
        for (EGameType value : EGameType.values()) {
            int majorType = CommonUtil.getMajorTypeByGameType(value.getGameTypeId());
            MAJOR_TYPE_ID_SET.computeIfAbsent(majorType, k -> new ArrayList<>()).add(value);
        }
    }

    public class Common {
        //redisLock重试次数
        public static final int REDIS_LOCK_TRY_COUNT = 5;
        public static final int REDIS_TRANSACTION_TRY_COUNT = 7;

        public static final String ENCODING = "UTF-8";

        //玩家起始id
        public static final long defaultPlayerBeginId = 1000000;

        //最小房间id
        public static final int ROOM_ID_MIN = 100000;
        //最大房间id
        public static final int ROOM_ID_MAX = 999999;
    }

    public interface Item{
        //可使用道具
        int TYPE_CAN_USE = 1;
        //可合成道具
        int TYPE_CAN_COMPOUND = 2;
        //货币类道具
        int TYPE_MONEY = 3;

        int ID_GOLD = 1030001;
        int ID_DIAMOND = 1030002;

        int PROP_MAX = -1;
    }

    public interface MarqueeType{
        //系统通知
        int SYSTEM_MSG = 100;
        //玩家交易
        int PLAYER_EXCHANGE = 200;
        //活动公告
        int AVTIVITY = 300;
        //玩家赢奖
        int PLAYER_WIN = 400;
    }


    public interface Redis {
        //游戏状态rediskey
        String GAME_STATUS_KEY = "gm:gamelistconfig";
        // 加锁尝试次数
        int LOCK_TRY_TIMES = 5;
        // 每次尝试花费的毫秒数
        int PER_TRY_TAKE_MILE_TIME = 5;
    }

    //登录类型
    public class LoginType {
        //游客
        public static final int GUEST = 0;
        //手机
        public static final int PHONE = 1;
        //google邮箱
        public static final int GOOGLE_MAIL = 2;
    }

    public class Gender {
        public static final byte WOMAN = 0;
        public static final byte MAN = 1;
        public static final byte OTHER = 2;
    }
}
