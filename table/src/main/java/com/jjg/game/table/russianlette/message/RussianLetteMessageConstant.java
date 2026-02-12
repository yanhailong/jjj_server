package com.jjg.game.table.russianlette.message;

import com.jjg.game.common.constant.MessageConst;

/**
 * 俄罗斯转盘消息常量
 *
 * @author 2CL
 */
public interface  RussianLetteMessageConstant {

    int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface ReqMsgBean {
        // 请求俄罗斯转盘房间摘要信息列表
        int REQ_RUSSIAN_LETTE_SUMMARY_LIST = BASE_MSG_PREFIX | 0x01;
        // 请求更新俄罗斯转盘房间单个摘要信息
        int REQ_RUSSIAN_LETTE_SUMMARY = BASE_MSG_PREFIX | 0x02;
        // 请求俄罗斯转盘房间信息
        int REQ_RUSSIAN_LETTE_INFO = BASE_MSG_PREFIX | 0x04;
        // 请求进入房间
        int REQ_JOIN_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x05;
        // 请求退出房间
        int REQ_EXIT_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x06;
        // 请求房间返回
        int REQ_SWITCH_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x07;
        // 请求查看其他房间
        int REQ_RUSSIAN_LETTE_OTHER_SUMMARY_LIST = BASE_MSG_PREFIX | 0x08;
    }

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_RUSSIAN_LETTE_TABLE_INFO = BASE_MSG_PREFIX | 0x81;
        // 房间结算通知
        int RESP_RUSSIAN_LETTE_SETTLEMENT = BASE_MSG_PREFIX | 0x82;
        // 返回俄罗斯转盘房间摘要列表
        int RESP_RUSSIAN_LETTE_SUMMARY_LIST = BASE_MSG_PREFIX | 0x83;
        // 返回俄罗斯转盘房间摘要
        int RESP_RUSSIAN_LETTE_SUMMARY = BASE_MSG_PREFIX | 0x84;
        // 进入房间的信息
        int RESP_RUSSIAN_LETTE_INFO = BASE_MSG_PREFIX | 0x85;
        // 进入房间返回
        int RESP_JOIN_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x86;
        // 退出房间返回
        int RESP_EXIT_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x87;
        // 返回切换房间返回
        int RESP_SWITCH_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x88;
        // 返回查看其他房间
        int RESP_RUSSIAN_LETTE_OTHER_SUMMARY_LIST = BASE_MSG_PREFIX | 0x89;
    }
}
