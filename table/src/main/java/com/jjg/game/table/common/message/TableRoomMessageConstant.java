package com.jjg.game.table.common.message;

import com.jjg.game.common.constant.MessageConst;

import static com.jjg.game.common.constant.MessageConst.MessageTypeDef.BET_GENERAL_TYPE;

/**
 * 下注类房间游戏消息常量
 *
 * @author 2CL
 */
public interface TableRoomMessageConstant {

    int BASE_MSG_PREFIX = BET_GENERAL_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface ReqMsgBean {
        // 请求房间下注
        int REQ_BET = BASE_MSG_PREFIX | 0x01;
        // 请求玩家信息
        int REQ_TABLE_PLAYER_INFO = BASE_MSG_PREFIX | 0x03;
        // 请求房间基本信息
        int REQ_ROOM_BASE_INFO = BASE_MSG_PREFIX | 0x04;
    }

    interface RespMsgBean {
        // 下注返回消息bean
        int RESP_BET = BASE_MSG_PREFIX | 0x81;
        // 返回玩家信息
        int RESP_TABLE_PLAYER_INFO = BASE_MSG_PREFIX | 0x83;
        // 通知玩家进入房间
        int NOTIFY_PLAYER_JOIN_ROOM = BASE_MSG_PREFIX | 0x88;
        // 通知阶段变化
        int NOTIFY_PHASE_CHANG_INFO = BASE_MSG_PREFIX | 0x89;
        // 通知阶段变化
        int NOTIFY_TABLE_EXIT_ROOM = BASE_MSG_PREFIX | 0x90;
        // 通知阶段变化
        int NOTIFY_TABLE_LONG_TIME_NO_OPERATE = BASE_MSG_PREFIX | 0x91;
    }
}
