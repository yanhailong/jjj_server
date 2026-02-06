package com.jjg.game.common.constant;

/**
 * 消息常量类
 *
 * @author 2CL
 * @since 1.0
 */
public interface MessageConst {

    interface MessageCommon {
        //右移位数
        int RIGHT_MOVE = 12;
        //与计算
        int MESSAGE_CAL = 0xFFF;
    }

    /**
     * 消息类型定义
     */
    interface MessageTypeDef {
        int TO_CLIENT_TYPE = 0x1;
        int SESSION_TYPE = 0x2;
        int CERTIFY_MESSAGE_TYPE = 0x3;
        int CORE_MESSAGE_TYPE = 0x4;
        // 房间通用消息类型
        int ROOM_TYPE = 0x5;

        //大厅
        int HALL_TYPE = 0x6;

        //美元快递
        int DOLLAR_EXPRESS_TYPE = 0x7;
        //森林人
        int WOODS_MAN_TYPE = 0x8;
        //超级明星
        int SUPER_STAR_TYPE = 0x9;
        //野牛财富
        int BUFFALO_WEALTH_TYPE = 0xA;
        //消防车
        int FIRE_CAR_TYPE = 0xB;
        //女武神
        int WOMAN_GOD_TYPE = 0xC;
        //麻将胡了
        int MAHJIONG_WIN_TYPE = 0xD;
        //招财猫
        int FORTUNE_CAT_TYPE = 0xE;
        //加勒比海盗
        int PIRATES_CARIBBEAN_TYPE = 0xF;
        //苹果机-水果
        int APPLE_FRUITS_TYPE = 0x10;
        //苹果机-动物
        int APPLE_ANIMAL_TYPE = 0x11;
        //夺宝黄金城
        int GOLD_CITY_TYPE = 0x12;


        //红黑大战
        int RED_BLACK_WAR_TYPE = 0x13;
        //龙虎斗
        int LOONG_TIGER_WAR_TYPE = 0x14;
        //捕鱼
        int CATCH_FISH_TYPE = 0x15;
        //飞禽走兽
        int BIRDS_ANIMAL_TYPE = 0x16;
        //豪车俱乐部
        int GOOD_CAR_CLUB_TYPE = 0x17;
        //百家乐
        int BACCARAT_TYPE = 0x18;
        //骰宝
        int DICE_BABY_TYPE = 0x19;
        //越南色碟
        int VIETNAM_SEXY_DISK_TYPE = 0x1A;
        //大小骰宝
        int SIZE_DICE_BABY_TYPE = 0x1B;
        //鱼虾蟹
        int FISH_SHRIMP_CRAB_TYPE = 0x1C;
        //俄罗斯转盘
        int RUSSIAN_ROULETTE_TYPE = 0x50;

        //21点
        int BLACK_JACK_TYPE = 0x1D;
        //德州
        int TEXAS_TYPE = 0x1E;
        //拉斯维加斯拼三张
        int VEGAS_THREE_TYPE = 0x1F;

        // 下注房间对战类通用协议类型
        int BET_GENERAL_TYPE = 0x20;

        // 服务器之间的通讯协议
        int TO_SERVER_CONST_TYPE = 0x21;


        // 下注房间对战类通用协议类型
        int POKER_GENERAL_TYPE = 0x22;
        // 好友房
        int FRIEND_ROOM_TYPE = 0x23;


        //财神
        int WEALTH_GOD = 0x24;
        //西游
        int WEST_JOURNEY = 0x25;
        //埃及艳后
        int CLEOPATRA = 0x26;

        //活动
        int ACTIVITY = 0x27;

        //小游戏
        int MINIGAME = 0x28;

        //商城
        int SHOP_TYPE = 0x29;

        //圣诞狂欢夜
        int CHRISTMAS_NIGHT_TYPE = 0x2A;
        //篮球巨星
        int BASKETBALL_SUPERSTAR = 0x2B;
        //SLOTS通用
        int SLOTS_COMMON = 0x2C;
        //寒冰王座
        int FROZEN_THRONE = 0x2D;

        //任务
        int TASK_TYPE = 0x30;
        //积分大奖
        int POINTS_AWARD = 0x31;

        //狼月
        int WOLF_MOON = 0x32;
        //热血足球
        int HOT_SOCCER = 0x33;
        //金蛇招财
        int GOLD_SNAKE_FORTUNE = 0x34;
        //金钱兔
        int MONEY_RABBIT = 0x35;
        //德古拉黑暗财富
        int DEGULA_WEALTH = 0x36;
        //宙斯VS哈迪斯
        int ZEUS_VS_HADES = 0x37;
        //杰克船长
        int CAPTAIN_JACK = 0x38;
        //雷神
        int THOR = 0x39;
        //蒸汽时代
        int STEAM_AGE = 0x3A;
        //财富银行
        int WEALTH_BANK = 0x3B;
        //神马飞扬
        int PEGASUS_UNBRIDLE = 0x3C;
        //鼠鼠福福
        int LUCKY_MOUSE = 0x3D;
        //象财神
        int ELEPHANT_GOD = 0x3E;
        //十倍金牛
        int TENFOLD_GOLDEN_BULL = 0x3F;
        //虎虎生财
        int TIGER_BRINGS_RICHES = 0x40;
        //王牌dj
        int ACE_DJ = 0x41;
        //绿巨人
        int HULK = 0x42;
        //恶魔之子
        int DEMON_CHILD = 0x43;

        int TO_SOUTH = 0x44;
    }

    interface ToClientConst {
        int BASE_MSG_PREFIX = MessageTypeDef.TO_CLIENT_TYPE << MessageCommon.RIGHT_MOVE;
        int REQ_HEART_BEAT = BASE_MSG_PREFIX | 0x01;
        int RES_HEART_BEAT = BASE_MSG_PREFIX | 0x02;
        int NOTICE_SERVER_STATUS = BASE_MSG_PREFIX | 0x03;
        int NOTIFY_KICK_OUT = BASE_MSG_PREFIX | 0x05;
    }

    /**
     * session相关
     */
    interface SessionConst {
        int BASE_MSG_PREFIX = MessageTypeDef.SESSION_TYPE << MessageCommon.RIGHT_MOVE;
        int NOTIFY_SESSION_QUIT = BASE_MSG_PREFIX | 0x01;
        int RES_NOTIFY_SESSION_VERIFYPASS = BASE_MSG_PREFIX | 0x02;
        //int NOTIFY_SESSION_CREATE = 0x2004;
        int NOTIFY_SESSION_ENTER = BASE_MSG_PREFIX | 0x04;
        int NOTIFY_SWITCH_NODE = BASE_MSG_PREFIX | 0x05;
        int NOTIFY_SESSION_LOGOUT = BASE_MSG_PREFIX | 0x06;
        int NOTIFY_SESSION_KICKOUT = BASE_MSG_PREFIX | 0x07;
        int CLUSTER_CONNECT_REGISTER = BASE_MSG_PREFIX | 0x08;
        //广播消息
        int BROADCAST_MSG = (MessageTypeDef.SESSION_TYPE << 8) | 0x9;
        // rpc请求消息
        int RPC_REQ_SERVICE_DATA_CARRIER = BASE_MSG_PREFIX | 0x0A;// rpc请求消息
        int RPC_RES_SERVICE_DATA_CARRIER = BASE_MSG_PREFIX | 0x0B;
    }

    /**
     * 验证
     */
    interface CertifyMessage {
        int BASE_MSG_PREFIX = MessageTypeDef.CERTIFY_MESSAGE_TYPE << MessageCommon.RIGHT_MOVE;

        //登录
        int REQ_LOGIN = BASE_MSG_PREFIX | 0x01;
        int RES_LOGIN = BASE_MSG_PREFIX | 0x02;
    }

    /**
     * 公共
     */
    interface CoreMessage {
        int BASE_MSG_PREFIX = MessageTypeDef.CORE_MESSAGE_TYPE << MessageCommon.RIGHT_MOVE;
        //gm
        int REQ_GM = BASE_MSG_PREFIX | 0x1;
        int RES_GM = BASE_MSG_PREFIX | 0x2;

        //选择场次
        int REQ_CHOOSE_WARE = BASE_MSG_PREFIX | 0x3;
        int RES_CHOOSE_WARE = BASE_MSG_PREFIX | 0x4;

        //通知玩家基础信息变化
        int NOTICE_BASE_INFO_CHANGE = BASE_MSG_PREFIX | 0x99;
        //推送跑马灯
        int NOTICE_MARQUEE = BASE_MSG_PREFIX | 0x9A;
        //停止跑马灯
        int NOTICE_STOP_MARQUEE = BASE_MSG_PREFIX | 0x9B;
        //通知踢人
        int NOTIFY_EXIT = BASE_MSG_PREFIX | 0x9C;
        //通知功能开放
        int NOTIFY_FUNC_OPEN = BASE_MSG_PREFIX | 0x9D;
        //请求确认玩家当前处于哪个场景中
        int REQ_CONFIRM_PLAYER_SCENE = BASE_MSG_PREFIX | 0x9E;
        //确认玩家当前处于哪个场景中
        int RES_CONFIRM_PLAYER_SCENE = BASE_MSG_PREFIX | 0x9F;
        //请求小红点数据
        int REQ_RED_DOT = BASE_MSG_PREFIX | 0x10;
        //通知红点数据更新
        int NOTIFY_RED_DOT = BASE_MSG_PREFIX | 0x11;
        //通知道具掉落
        int NOTIFY_ITEM_DROP = BASE_MSG_PREFIX | 0x12;
        //通知多语言提示弹窗
        int NOTIFY_TIP = BASE_MSG_PREFIX | 0x13;
        //通知玩家升级奖励
        int NOTIFY_PLAYER_LEVEL_UP = BASE_MSG_PREFIX | 0x14;
        //请求订阅消息推送主题操作
        int REQ_SUBSCRIBE_TOPIC = BASE_MSG_PREFIX | 0x15;
        //请求订阅消息推送主题操作回复
        int RES_SUBSCRIBE_TOPIC = BASE_MSG_PREFIX | 0x16;

        //请求预下单
        int REQ_GENERATE_ORDER = BASE_MSG_PREFIX | 0x17;
        //返回预下单
        int RES_GENERATE_ORDER = BASE_MSG_PREFIX | 0x18;

        //获取玩家的账户信息
        int REQ_PLAYER_MONEY = BASE_MSG_PREFIX | 0x19;
        //返回玩家的账户信息
        int RES_PLAYER_MONEY = BASE_MSG_PREFIX | 0x1A;
        //通知玩家货币变化
        int NOTIFY_MONEY_CHANGE = BASE_MSG_PREFIX | 0x1B;
        //通知玩家充值后的信息
        int NOTIFY_PAY_INFO = BASE_MSG_PREFIX | 0x1C;
        //请求配置数据
        int REQ_EXCEL_INFOS = BASE_MSG_PREFIX | 0x1D;
        int RES_EXCEL_INFOS = BASE_MSG_PREFIX | 0x1F;

        //请求背包数据
        int REQ_GET_PACK = BASE_MSG_PREFIX | 0x20;
        int RES_GET_PACK = BASE_MSG_PREFIX | 0x21;
    }

    interface RoomMessage {
        int BASE_MSG_PREFIX = MessageTypeDef.ROOM_TYPE << MessageCommon.RIGHT_MOVE;
        //退出游戏
        int REQ_EXIT_GAME = BASE_MSG_PREFIX | 0x2;
        int RES_EXIT_GAME = BASE_MSG_PREFIX | 0x3;
    }

    /**
     * 服务器之间
     */
    interface ToServer {
        int BASE_MSG_PREFIX = MessageTypeDef.TO_SERVER_CONST_TYPE << MessageCommon.RIGHT_MOVE;
        int REQ_REFRESH_GAME_STATUS = BASE_MSG_PREFIX | 0x1;

        //通知slots节点，结果库变更
        int NOTICE_SLOTS_LIB_CHANGE = BASE_MSG_PREFIX | 0x2;

        //向大厅和游戏节点推送跑马灯
        int NOTICE_MARQUEE_HALL_MASTER = BASE_MSG_PREFIX | 0x3;
        //向大厅和游戏节点推送停止跑马灯
        int NOTICE_STOP_MARQUEE_HALL_MASTER = BASE_MSG_PREFIX | 0x4;
        //向其他节点发送全服踢人
        int NOTICE_ALL_KICK_OUT = BASE_MSG_PREFIX | 0x5;
        //通知大厅节点更新轮播数据
        int NOTICE_ALL_UPDATE_CAROUSEL = BASE_MSG_PREFIX | 0x6;

        //通知生成结果库
        int NOTICE_GENERATE_LIB = BASE_MSG_PREFIX | 0x7;
        //通知商城商品变更
        int NOTICE_SHOP_PRODUCT_CHANGE = BASE_MSG_PREFIX | 0x8;

        //玩家充值成功
        int NOTIFY_PLAYER_RECHARGE = BASE_MSG_PREFIX | 0x9;

        //请求活动信息
        int REQ_ACTIVITY_INFOS = BASE_MSG_PREFIX | 0xA;

        //夺宝奇兵更新库存
        int NOTIFY_LUCKY_TREASURE_UPDATE_STOCK = BASE_MSG_PREFIX | 0xB;

        //配置更新通知
        int CONFIG_UPDATE = BASE_MSG_PREFIX | 0xC;

        //响应活动信息
        int RES_ACTIVITY_INFOS = BASE_MSG_PREFIX | 0xD;

        //节点信息变化
        int NOTIFY_GAME_NODE_CHANGE = BASE_MSG_PREFIX | 0xE;

        //通知服务器配置表变化
        int NOTIFY_EXCEL_CHANGE = BASE_MSG_PREFIX | 0x10;
        //通知服务器加载登录配置
        int NOTIFY_LOAD_LOGIN_CONFIG = BASE_MSG_PREFIX | 0x11;
        //通知服务器加载黑名单
        int NOTIFY_LOAD_BLACK_LIST = BASE_MSG_PREFIX | 0x12;
        //通知修改玩家金币
        int NOTIFY_GOLD_OPERATE = BASE_MSG_PREFIX | 0x13;
        //通知加载公告配置
        int NOTIFY_LOAD_NOTICE_LIST = BASE_MSG_PREFIX | 0x14;
    }
}
