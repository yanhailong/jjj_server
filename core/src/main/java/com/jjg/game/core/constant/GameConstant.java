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

        public static final String ENCODING = "UTF-8";

        //玩家起始id
        public static final long defaultPlayerBeginId = 1000000;
    }

    public interface AccountStatus {
        int NORMAL = 0;
        int BAN = 1;
    }

    public interface Item {
        //可使用道具
        int TYPE_CAN_USE = 1;
        //可合成道具
        int TYPE_CAN_COMPOUND = 2;

        int TYPE_GOLD = 99;
        int TYPE_DIAMOND = 98;
    }

    public interface Marquee {
        //系统通知
        int SYSTEM_MSG = 100;
        //玩家交易
        int PLAYER_EXCHANGE = 200;
        //活动公告
        int AVTIVITY = 300;
        //玩家赢奖
        int PLAYER_WIN = 400;

        //客户端原始展示类型
        int CLIENT_NORMAL_TYPE = 0;
        //客户端需要进行多语言参数匹配的类型
        int CLIENT_LANG_TYPE = 1;

        //玩家中奖跑马灯，间隔时间
        int PLAYER_WIN_INTERVAL = 8;
        //活动开始跑马灯，间隔时间
        int ACTIVITY_INTERVAL = 8;
    }


    public interface Redis {
        //游戏状态rediskey
        String GAME_STATUS_KEY = "gm:gamelistconfig";
        // 加锁尝试次数
        int LOCK_TRY_TIMES = 50;
        // 每次尝试花费的毫秒数
        int PER_TRY_TAKE_MILE_TIME = 10;
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

    //性别
    public class Gender {
        //女
        public static final byte WOMAN = 0;
        //男
        public static final byte MAN = 1;
        //其他
        public static final byte OTHER = 2;
    }

    public class Mail {
        //未阅读
        public static final int STAUTS_NOT_READ = 0;
        //已阅读
        public static final int STAUTS_READ = 1;
        //已领取
        public static final int STAUTS_GET_ITEMS = 2;
    }

    public class GlobalConfig {
        //vip最大开放等级
        public static final int ID_VIP_OPEN_MAX_LEVEL = 1;
        //基础经验倍率
        public static final int ID_BASE_EXP_PROP = 2;
        //基础流水倍率
        public static final int ID_BASE_STATEMENT_PROP = 3;

        //5个大奖
        public static final int ID_SWEET = 6;
        public static final int ID_BIG = 7;
        public static final int ID_MEGA = 8;
        public static final int ID_EPIC = 9;
        public static final int ID_LEGENDARY = 10;

        //默认装备的配置id
        public static final int DEFAULT_AVATAR_CFG_ID = 14;

        //默认的邮件有效期
        public static final int DEFAULT_MAIL_VALID_TIME = 18;
    }

    public class PlayerBuff {
        //经验加成
        public static final int TYPE_EXP_PROP = 1;
        //流水加成
        public static final int TYPE_STATEMENT_PROP = 1;
    }

    public class Language {
        //原始展示
        public static final int TYPE_ORIGINAL = 0;
        //多语言
        public static final int TYPE_LANGUAGE_MATCH = 1;
    }

    public interface RoomTypeCons {
        // 好友房 房间类型开始...
        int FRIEND_ROOM_TYPE_START = 10;
    }
}
