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
        int TO_SERVER_CONST_TYPE = 0x5;

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


        //21点
        int BLACK_JACK_TYPE = 0x1D;
        //德州
        int TEXAS_TYPE = 0x1E;
        //拉斯维加斯拼三张
        int VEGAS_THREE_TYPE = 0x1F;
    }

    interface ToClientConst {
        int BASE_MSG_PREFIX = MessageTypeDef.TO_CLIENT_TYPE << MessageCommon.RIGHT_MOVE;
        int REQ_HEART_BEAT = BASE_MSG_PREFIX | 0x01;
        int RES_HEART_BEAT = BASE_MSG_PREFIX | 0x02;
        int NOTICE_SERVER_STATUS = BASE_MSG_PREFIX | 0x03;
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
        //登录
        int REQ_GM = BASE_MSG_PREFIX | 0x1;
        int RES_GM = BASE_MSG_PREFIX | 0x2;
        //通知金钱变化
        int NOTICE_MONEY_CHANGE = BASE_MSG_PREFIX | 0x99;
    }

    /**
     * 服务器之间
     */
    interface ToServer {
//        int TYPE = MessageTypeDef.TO_SERVER_CONST_TYPE;
    }
}
