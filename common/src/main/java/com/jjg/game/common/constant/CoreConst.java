package com.jjg.game.common.constant;


/**
 * 系统级常量池
 *
 * @since 1.0
 */
public class CoreConst {

    /**
     * 游戏主分类
     */
    public static class GameMajorType {
        //slots类游戏
        public static final int SLOTS = 1;
        //押注类游戏
        public static final int TABLE = 2;
        //扑克类游戏
        public static final int POKER = 3;
    }

    public static class GameType {
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
        //夺宝黄金城
        public static final int GOLD_CITY = 101100;
        //财神
        public static final int WEALTH_GOD = 101200;
        //西游
        public static final int WEST_JOURNEY = 101300;
        //埃及艳后
        public static final int CLEOPATRA = 101400;
        //狼月
        public static final int WOLF_MOON = 101500;
        //热血足球
        public static final int HOT_SOCCER = 101600;
        //圣诞狂欢夜
        public static final int CHRISTMAS_PARTY = 101700;
        //篮球巨星
        public static final int BASKETBALL_STAR = 101800;
        //德古拉黑暗财富
        public static final int DEGULA_WEALTH = 101900;
        //宙斯VS哈迪斯
        public static final int ZEUS_VS_HADES = 102000;
        //杰克船长
        public static final int CAPTAIN_JACK = 102100;
        //雷神
        public static final int THOR = 102200;
        //蒸汽时代
        public static final int STEAM_AGE = 102300;
        //财富银行
        public static final int WEALTH_BANK = 102400;
        //寒冰王座
        public static final int FROZEN_THRONE = 102500;
        //象财神
        public static final int ELEPHANT_GOD = 102700;
        //王牌Dj
        public static final int ACE_DJ = 102800;
        //绿巨人
        public static final int HULK = 103100;
        //神马飞扬
        public static final int PEGASUS_UNBRIDLE = 103400;
        //金蛇招财
        public static final int GOLD_SNAKE_FORTUNE = 103500;
        //金钱兔
        public static final int MONEY_RABBIT = 103501;
        //鼠鼠福福
        public static final int LUCKY_MOUSE = 103600;
        //十倍金牛
        public static final int TENFOLD_GOLDEN_BULL = 103700;
        //恶魔之子
        public static final int DEMON_CHILD = 103800;
        // pan jin lian
        public static final int PAN_JIN_LIAN = 103900;
        //虎虎生财
        public static final int TIGER_BRINGS_RICHES = 103401;
        //愤怒的小鸟
        public static final int ANGRY_BIRDS = 103900;

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
        //苹果机-水果
        public static final int APPLE_FRUITS = 201000;
        //苹果机-动物
        public static final int APPLE_ANIMAL = 201001;
        //红包扫雷
        public static final int RED_PACKET = 201100;
        //俄罗斯转盘
        public static final int RUSSIAN_ROULETTE = 201200;


        //21点
        public static final int BLACK_JACK = 300100;
        //德州
        public static final int TEXAS = 300200;
        //拉斯维加斯拼三张
        public static final int VEGAS_THREE = 300300;
        //南方前进
        public static final int TO_SOUTH = 300400;
    }

    public static class Common {
        // 本系统统一文件分隔符
        public static final String SEPARATOR = "/";

        //redis如果发生锁竞争,则设置重试最大次数
        public static final int REDIS_TRY_COUNT = 10;
        //MONGO插入数据失败,则设置重试最大次数
        public static final int MONGO_TRY_COUNT = 10;

        //excel根目录
        public static final String SAMPLE_ROOT_PATH = "resources/sample/";

        // 项目package路径
        public static final String BASE_PROJECT_PACKAGE_PATH = "com.jjg.game";
    }

    public static class Session {
        // session广播批量发送限制，超过此值开始分批发送
        public static final int SESSION_BROADCAST_BATCH_LIMIT = 100;
    }
}
