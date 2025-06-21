package com.jjg.game.common.constant;

/**
 * @since 1.0
 */
public interface MessageConst {
    interface MessageCommon{
        //右移位数
        int RIGHT_MOVE = 12;
        //与计算
        int MESSAGE_CAL = 0xFFF;
    }

    interface SessionConst {
        int TYPE = 0x2;
        int NOTIFY_SESSION_QUIT = 0x2001;
        int RES_NOTIFY_SESSION_VERIFYPASS = 0x2002;

        //int NOTIFY_SESSION_CREATE = 0x2004;
        int NOTIFY_SESSION_ENTER = 0x2004;
        int NOTIFY_SWITCH_NODE = 0x2005;
        int NOTIFY_SESSION_LOGOUT = 0x2006;
        int NOTIFY_SESSION_KICKOUT = 0x2007;
        int CLUSTER_CONNECT_REGISTER = 0x2008;
        //广播消息
        int BROADCAST_MSG = 0x209;
    }

    interface ToClientConst {
        int TYPE = 0x1;

        int REQ_HEART_BEAT = 0x1001;
        int RES_HEART_BEAT = 0x1002;

        int NOTICE_SERVER_STATUS = 0x1003;
    }

    interface CertifyMessage {
        int TYPE = 0x3;
        //登录
        int REQ_LOGIN = 0x3001;
        int RES_LOGIN = 0x3002;
    }

    interface CoreMessage {
        int TYPE = 0x4;
        //登录
        int REQ_GM = 0x4001;
        int RES_GM = 0x4002;

        //通知金钱变化
        int NOTICE_MONEY_CHANGE = 0x4099;
    }

    interface ToServer {
        int TYPE = 0x5;

    }
}
