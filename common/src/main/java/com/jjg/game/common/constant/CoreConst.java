package com.jjg.game.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统级常量池
 * 
 * @since 1.0
 */
public class CoreConst {

    //游戏类型和消息类型的对应关系
    public static final Map<Integer, Integer> gameTypeToMsgTypeMap = new HashMap<>();

    static {
        gameTypeToMsgTypeMap.put(GameType.DOLLAR_EXPRESS,MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE);
        gameTypeToMsgTypeMap.put(GameType.WOODS_MAN,MessageConst.MessageTypeDef.WOODS_MAN_TYPE);
        gameTypeToMsgTypeMap.put(GameType.SUPER_STAR,MessageConst.MessageTypeDef.SUPER_STAR_TYPE);
        gameTypeToMsgTypeMap.put(GameType.BUFFALO_WEALTH,MessageConst.MessageTypeDef.BUFFALO_WEALTH_TYPE);
        gameTypeToMsgTypeMap.put(GameType.FIRE_CAR,MessageConst.MessageTypeDef.FIRE_CAR_TYPE);
        gameTypeToMsgTypeMap.put(GameType.WOMAN_GOD,MessageConst.MessageTypeDef.WOMAN_GOD_TYPE);
        gameTypeToMsgTypeMap.put(GameType.MAHJIONG_WIN,MessageConst.MessageTypeDef.MAHJIONG_WIN_TYPE);
        gameTypeToMsgTypeMap.put(GameType.FORTUNE_CAT,MessageConst.MessageTypeDef.FORTUNE_CAT_TYPE);
        gameTypeToMsgTypeMap.put(GameType.PIRATES_CARIBBEAN,MessageConst.MessageTypeDef.PIRATES_CARIBBEAN_TYPE);
        gameTypeToMsgTypeMap.put(GameType.APPLE_FRUITS,MessageConst.MessageTypeDef.APPLE_FRUITS_TYPE);
        gameTypeToMsgTypeMap.put(GameType.APPLE_ANIMAL,MessageConst.MessageTypeDef.APPLE_ANIMAL_TYPE);
        gameTypeToMsgTypeMap.put(GameType.GOLD_CITY,MessageConst.MessageTypeDef.GOLD_CITY_TYPE);

        gameTypeToMsgTypeMap.put(GameType.RED_BLACK_WAR,MessageConst.MessageTypeDef.RED_BLACK_WAR_TYPE);
        gameTypeToMsgTypeMap.put(GameType.LOONG_TIGER_WAR,MessageConst.MessageTypeDef.LOONG_TIGER_WAR_TYPE);
        gameTypeToMsgTypeMap.put(GameType.CATCH_FISH,MessageConst.MessageTypeDef.CATCH_FISH_TYPE);
        gameTypeToMsgTypeMap.put(GameType.BIRDS_ANIMAL,MessageConst.MessageTypeDef.BIRDS_ANIMAL_TYPE);
        gameTypeToMsgTypeMap.put(GameType.GOOD_CAR_CLUB,MessageConst.MessageTypeDef.GOOD_CAR_CLUB_TYPE);
        gameTypeToMsgTypeMap.put(GameType.BACCARAT,MessageConst.MessageTypeDef.BACCARAT_TYPE);
        gameTypeToMsgTypeMap.put(GameType.DICE_BABY,MessageConst.MessageTypeDef.DICE_BABY_TYPE);
        gameTypeToMsgTypeMap.put(GameType.VIETNAM_SEXY_DISK,MessageConst.MessageTypeDef.VIETNAM_SEXY_DISK_TYPE);
        gameTypeToMsgTypeMap.put(GameType.SIZE_DICE_BABY,MessageConst.MessageTypeDef.SIZE_DICE_BABY_TYPE);
        gameTypeToMsgTypeMap.put(GameType.FISH_SHRIMP_CRAB,MessageConst.MessageTypeDef.FISH_SHRIMP_CRAB_TYPE);

        gameTypeToMsgTypeMap.put(GameType.BLACK_JACK,MessageConst.MessageTypeDef.BLACK_JACK_TYPE);
        gameTypeToMsgTypeMap.put(GameType.TEXAS,MessageConst.MessageTypeDef.TEXAS_TYPE);
        gameTypeToMsgTypeMap.put(GameType.VEGAS_THREE,MessageConst.MessageTypeDef.VEGAS_THREE_TYPE);
    }

    public class GameType{
        //美元快递
        public static final int DOLLAR_EXPRESS = 100100;
        //森林人
        public static final int WOODS_MAN = 100200;
        //超级明星
        public static final int SUPER_STAR = 100300;
        //野牛财富
        public static final int BUFFALO_WEALTH = 100400;
        //消防车
        public static final int FIRE_CAR = 100500;
        //女武神
        public static final int WOMAN_GOD = 100600;
        //麻将胡了
        public static final int MAHJIONG_WIN = 100700;
        //招财猫
        public static final int FORTUNE_CAT = 100800;
        //加勒比海盗
        public static final int PIRATES_CARIBBEAN = 100900;
        //苹果机-水果
        public static final int APPLE_FRUITS = 101000;
        //苹果机-动物
        public static final int APPLE_ANIMAL = 101001;
        //夺宝黄金城
        public static final int GOLD_CITY = 101100;


        //红黑大战
        public static final int RED_BLACK_WAR = 200100;
        //龙虎斗
        public static final int LOONG_TIGER_WAR = 200101;
        //捕鱼
        public static final int CATCH_FISH = 200200;
        //飞禽走兽
        public static final int BIRDS_ANIMAL = 200300;
        //豪车俱乐部
        public static final int GOOD_CAR_CLUB = 200400;
        //百家乐
        public static final int BACCARAT = 200500;
        //骰宝
        public static final int DICE_BABY = 200600;
        //越南色碟
        public static final int VIETNAM_SEXY_DISK = 200700;
        //大小骰宝
        public static final int SIZE_DICE_BABY = 200800;
        //鱼虾蟹
        public static final int FISH_SHRIMP_CRAB = 200900;


        //21点
        public static final int BLACK_JACK = 300100;
        //德州
        public static final int TEXAS = 300200;
        //拉斯维加斯拼三张
        public static final int VEGAS_THREE = 300300;
    }

    public class Common{
        // 本系统统一文件分隔符
        public static final String SEPARATOR = "/";

        //redis如果发生锁竞争,则设置重试最大次数
        public static final int REDIS_TRY_COUNT = 10;
    }
}
