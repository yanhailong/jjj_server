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
        int HALL_TYPE = 0x6;
        int DOLLAR_EXPRESS_TYPE = 0x7;
    }

    interface ToClientConst {
        int TYPE = MessageTypeDef.TO_CLIENT_TYPE;
        int BASE_MSG_PREFIX = TYPE << MessageCommon.RIGHT_MOVE;
        int REQ_HEART_BEAT = BASE_MSG_PREFIX | 0x01;
        int RES_HEART_BEAT = BASE_MSG_PREFIX | 0x02;
        int NOTICE_SERVER_STATUS = BASE_MSG_PREFIX | 0x03;
    }

    interface SessionConst {
        int TYPE = MessageTypeDef.SESSION_TYPE;
        int BASE_MSG_PREFIX = TYPE << MessageCommon.RIGHT_MOVE;
        int NOTIFY_SESSION_QUIT = BASE_MSG_PREFIX | 0x01;
        int RES_NOTIFY_SESSION_VERIFYPASS = BASE_MSG_PREFIX | 0x02;
        //int NOTIFY_SESSION_CREATE = 0x2004;
        int NOTIFY_SESSION_ENTER = BASE_MSG_PREFIX | 0x04;
        int NOTIFY_SWITCH_NODE = BASE_MSG_PREFIX | 0x05;
        int NOTIFY_SESSION_LOGOUT = BASE_MSG_PREFIX | 0x06;
        int NOTIFY_SESSION_KICKOUT = BASE_MSG_PREFIX | 0x07;
        int CLUSTER_CONNECT_REGISTER = BASE_MSG_PREFIX | 0x08;
        //广播消息
        int BROADCAST_MSG = (TYPE << 8) | 0x9;
    }

    interface CertifyMessage {
        int TYPE = MessageTypeDef.CERTIFY_MESSAGE_TYPE;
        int BASE_MSG_PREFIX = TYPE << MessageCommon.RIGHT_MOVE;
        //登录
        int REQ_LOGIN = BASE_MSG_PREFIX | 0x01;
        int RES_LOGIN = BASE_MSG_PREFIX | 0x02;
    }

    interface CoreMessage {
        int TYPE = MessageTypeDef.CORE_MESSAGE_TYPE;
        int BASE_MSG_PREFIX = TYPE << MessageCommon.RIGHT_MOVE;
        //登录
        int REQ_GM = BASE_MSG_PREFIX | 0x1;
        int RES_GM = BASE_MSG_PREFIX | 0x2;
        //通知金钱变化
        int NOTICE_MONEY_CHANGE = BASE_MSG_PREFIX | 0x99;
    }

    interface ToServer {
        int TYPE = MessageTypeDef.TO_SERVER_CONST_TYPE;
    }
}
