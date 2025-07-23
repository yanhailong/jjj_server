package com.jjg.game.room.message;

import com.jjg.game.common.constant.MessageConst;

/**
 * 房间通用流程消息定义ID,比如请求创建房间、请求加入好友房以及请求退出房间等等通用房间逻辑...
 *
 * @author 2CL
 */
public interface RoomMessageConstant {

    int BASE_ROOM_MSG_PREFIX = MessageConst.MessageTypeDef.ROOM_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    /**
     * 请求消息的bean
     */
    interface ReqMsgBean {
        int REQ_EXIT_ROOM = BASE_ROOM_MSG_PREFIX | 0x01;

        //退出游戏
        int REQ_EXIT_GAME = BASE_ROOM_MSG_PREFIX | 0x2;

    }

    /**
     * 返回消息的bean
     */
    interface RespMsgBean {
        //退出游戏
        int RES_EXIT_GAME = BASE_ROOM_MSG_PREFIX | 0x3;
        // 通知退出房间
        int NOTIFY_EXIT_ROOM = BASE_ROOM_MSG_PREFIX | 0x81;
        // 通知房间进入等待时间
        int NOTIFY_ROOM_WAIT_READY = BASE_ROOM_MSG_PREFIX | 0x82;
    }
}
