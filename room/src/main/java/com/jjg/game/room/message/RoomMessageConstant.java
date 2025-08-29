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
        // 请求上庄申请列表
        int REQ_BANKER_APPLY_LIST = BASE_ROOM_MSG_PREFIX | 0x01;
        // 退出游戏
        int REQ_EXIT_GAME = BASE_ROOM_MSG_PREFIX | 0x2;
        // 返回退出
        int RES_EXIT_GAME = BASE_ROOM_MSG_PREFIX | 0x3;
        // 申请上庄
        int REQ_APPLY_BANKER = BASE_ROOM_MSG_PREFIX | 0x04;
        // 申请下庄
        int REQ_CANCEL_BE_BANKER = BASE_ROOM_MSG_PREFIX | 0x05;
        // 请求修改庄家预付金币，只能增加，不能减少
        int REQ_EDIT_BANKER_PREDICATE_GOLD = BASE_ROOM_MSG_PREFIX | 0x06;
    }

    /**
     * 返回消息的bean
     */
    interface RespMsgBean {
        // 通知退出房间
        int NOTIFY_EXIT_ROOM = BASE_ROOM_MSG_PREFIX | 0x81;
        // 通知房间进入等待时间
        int NOTIFY_ROOM_WAIT_READY = BASE_ROOM_MSG_PREFIX | 0x82;
        // 申请上庄
        int RES_APPLY_BANKER = BASE_ROOM_MSG_PREFIX | 0x83;
        // 申请庄家玩家列表
        int RES_BANKER_APPLY_LIST = BASE_ROOM_MSG_PREFIX | 0x84;
        // 申请下庄返回
        int RES_CANCEL_BE_BANKER = BASE_ROOM_MSG_PREFIX | 0x85;
        // 通知游戏暂停
        int NOTIFY_GAME_PAUSE_ON_NEW_ROUND = BASE_ROOM_MSG_PREFIX | 0x86;
        // 返回庄家预付金币修改结果
        int RES_EDIT_BANKER_PREDICATE_GOLD = BASE_ROOM_MSG_PREFIX | 0x87;
    }
}
