package com.jjg.game.poker.game.blackjack.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/6/27 18:04
 */
public interface BlackJackConstant {

    interface Common {
        //完美点数
        int PERFECT_POINT = 21;
        //最大拿牌数
        int MAX_GET_CARD = 6;
        //下注修正时间
        int BET_FIX_TIME = 5000;
        //分牌修正时间
        int CUT_FIX_TIME = 500;
        //拿牌点数
        int GET_CARD_POINT = 17;
    }

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.BLACK_JACK_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //响应房间基础信息
        int REPS_BLACK_ROOM_BASE_INFO = BASE_MSG_PREFIX | 0x1;
        //通知下注结果
        int NOTIFY_BET_RESULT = BASE_MSG_PREFIX | 0x3;
        //通知21点结算信息
        int NOTIFY_BLACK_JACK_SETTLEMENT_INFO = BASE_MSG_PREFIX | 0x5;
        //通知玩家分牌信息
        int NOTIFY_CUT_CARD = BASE_MSG_PREFIX | 0x6;
        //通知玩家第一次发牌信息
        int NOTIFY_SEND_CARD_INFO = BASE_MSG_PREFIX | 0x7;
        //通知玩家拿牌信息
        int NOTIFY_BLACKJACK_PUT_CARD = BASE_MSG_PREFIX | 0x8;
        //通知玩家进行双倍押注
        int NOTIFY_BLACKJACK_DOUBLE_BET_INFO = BASE_MSG_PREFIX | 0x9;
        //响应玩家购买ACE
        int REPS_BLACKJACK_BUY_ACE = BASE_MSG_PREFIX | 0x10;
        //通知玩家停牌
        int NOTIFY_BLACKJACK_STOP_CARD = BASE_MSG_PREFIX | 0x11;
        //通知玩家开局结算
        int NOTIFY_BLACKJACK_SPECIAL_SETTLEMENT = BASE_MSG_PREFIX | 0x12;
    }
}
