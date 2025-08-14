package com.jjg.game.poker.game.texas.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/6/27 18:04
 */
public interface TexasConstant {

    interface Common {
        //最大轮次
        int MAX_ROUND = 4;
        //翻牌轮
        int FLIP_CARDS_ROUND = 2;
        //初始轮
        int INIT_ROUND = 1;
        //翻牌轮发牌数
        int SEND_CARD_NUM = 3;
        //翻牌后发牌数
        int ADD_CARDS = 1;
        //弃牌结算
        int DISCARD_SETTLEMENT = 1;
        //全all结算
        int ALL_SETTLEMENT = 2;
    }


    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.TEXAS_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //响应房间基础信息
        int REPS_ROOM_BASE_INFO = BASE_MSG_PREFIX | 0x1;
        //通知玩家第一轮信息
        int NOTIFY_PRE_FLOP_ROUND_INFO = BASE_MSG_PREFIX | 0x2;
        //通知德州扑克响应消息
        int NOTIFY_ALL_IN_SETTLEMENT_INFO = BASE_MSG_PREFIX | 0x3;
        //通知第一轮以外的轮次变化信息
        int NOTIFY_PUBLIC_CARD_CHANGE = BASE_MSG_PREFIX | 0x4;

        //请求下注
        int REQ_BET = BASE_MSG_PREFIX | 0x5;
        //通知下注信息
        int NOTIFY_BET = BASE_MSG_PREFIX | 0x6;
        //请求亮牌
        int REQ_SHOW_CARD = BASE_MSG_PREFIX | 0x7;
        //通知亮牌
        int NOTIFY_SHOW_CARD = BASE_MSG_PREFIX | 0x8;
        //通知玩家变化
        int NOTIFY_PLAYER_CHANGE = BASE_MSG_PREFIX | 0x9;
        //请求改变座位状态
        int REQ_CHANGE_SEAT_STATE = BASE_MSG_PREFIX | 0x10;
        //通知座位变化
        int NOTIFY_SEAT_STATE_CHANGE = BASE_MSG_PREFIX | 0x11;
        //请求换桌
        int REQ_CHANGE_TABLE = BASE_MSG_PREFIX | 0x12;
        //响应换桌
        int REPS_CHANG_TABLE = BASE_MSG_PREFIX | 0x13;
        //请求德州扑克历史记录
        int REQ_TEXAS_HISTORY = BASE_MSG_PREFIX | 0x14;
        //响应德州扑克历史记录
        int REQS_TEXAS_HISTORY = BASE_MSG_PREFIX | 0x15;
    }
}
