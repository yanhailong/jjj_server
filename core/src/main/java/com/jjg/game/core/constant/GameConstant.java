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
    public static final int ROBOT_ID_PRIME_NUMBER = 17;

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
        int ACTIVITY = 300;
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
        //任务redis等待时间
        int TASK_TIME = 10000;
        //通用redis等待时间
        int TIME = 500;
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
        public static final int STATUS_NOT_READ = 0;
        //已阅读
        public static final int STATUS_READ = 1;
        //已领取
        public static final int STATUS_GET_ITEMS = 2;

        public static final int ID_BIND_GOOGLE = 31;
        public static final int ID_BIND_FACEBOOK = 32;
        public static final int ID_BIND_PHONE = 33;
        public static final int ID_BIND_APPLE = 34;

        //积分大奖进度奖励
        public static final int ID_POINTS_AWARD = 40;
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

        //积分大奖转盘，每次消耗积分数
        public static final int POINTS_AWARDS_TURNTABLE_SPEND_SCORE = 40;
        //积分大奖：每日转盘默认初始次数上限
        public static final int POINTS_AWARDS_TURNTABLE_INIT_COUNT_LIMIT = 43;
        //货币符号
        public static final int ID_MONEY_SYMBOL = 100;
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

    //账号类型
    public class AccountType {
        //游客
        public static final int GUEST = 0;
        //认证用户
        public static final int VERIFIED = 1;
    }

    public interface VerCode {
        //验证码范围
        int CODE_MIN = 100000;
        int CODE_MAX = 999999;
    }

    public interface CommonDaoId{
        //客服链接
        int CUSTOMER_TABLE_ID = 1;
    }
}
