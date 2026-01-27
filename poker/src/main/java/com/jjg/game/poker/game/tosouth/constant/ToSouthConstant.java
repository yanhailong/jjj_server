package com.jjg.game.poker.game.tosouth.constant;

import com.jjg.game.common.constant.MessageConst;

public class ToSouthConstant {

    public interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.TO_SOUTH << MessageConst.MessageCommon.RIGHT_MOVE;
        int REQ_TURN_ACTION = BASE_MSG_PREFIX | 0x1;

        //响应房间基础信息
        int RESP_ROOM_BASE_INFO = BASE_MSG_PREFIX | 0xA;


        int NOTIFY_SEND_CARD_INFO = BASE_MSG_PREFIX | 0x11;
        int NOTIFY_SETTLEMENT_INFO = BASE_MSG_PREFIX | 0x12;
        int NOTIFY_TURN_ACTION_INFO = BASE_MSG_PREFIX | 0x13;
        int NOTIFY_BOMB_SETTLEMENT = BASE_MSG_PREFIX | 0x14;
    }
}
