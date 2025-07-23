package com.jjg.game.table.baccarat.message;

import com.jjg.game.common.constant.MessageConst;

import static com.jjg.game.common.constant.MessageConst.MessageTypeDef.BACCARAT_TYPE;

/**
 * 百家乐消息常量
 *
 * @author 2CL
 */
public interface BaccaratMessageConstant {

    int BASE_MSG_PREFIX = BACCARAT_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface ReqMsgBean {
        // 请求百家乐房间摘要信息列表
        int REQ_BACCARAT_TABLE_SUMMARY_LIST = BASE_MSG_PREFIX | 0x01;
        // 请求更新百家乐房间单个摘要信息
        int REQ_BACCARAT_TABLE_SUMMARY = BASE_MSG_PREFIX | 0x02;
        // 请求百家乐房间信息
        int REQ_BACCARAT_TABLE_INFO = BASE_MSG_PREFIX | 0x04;
        // 请求进入房间
        int REQ_JOIN_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x05;
        // 请求退出房间
        int REQ_EXIT_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x06;
    }

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_BACCARAT_TABLE_INFO = BASE_MSG_PREFIX | 0x81;
        // 返回百家乐房间摘要列表
        int RESP_BACCARAT_TABLE_SUMMARY_LIST = BASE_MSG_PREFIX | 0x82;
        // 返回百家乐房间摘要
        int RESP_BACCARAT_TABLE_SUMMARY = BASE_MSG_PREFIX | 0x83;
        // 进入房间的信息
        int RESP_BACCARAT_TABLE_INFO = BASE_MSG_PREFIX | 0x84;
        // 请求百家乐房间一句开始信息
        int NOTIFY_BACCARAT_TABLE_ROUND_START = BASE_MSG_PREFIX | 0x85;
        // 百家乐结算信息
        int NOTIFY_BACCARAT_TABLE_SETTLEMENT_INFO = BASE_MSG_PREFIX | 0x86;
        // 进入房间返回
        int RESP_JOIN_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x87;
        // 退出房间返回
        int RESP_EXIT_ROOM_IN_GAME = BASE_MSG_PREFIX | 0x88;
    }
}
