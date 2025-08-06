package com.jjg.game.poker.game.blackjack.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/6/27 18:04
 */
public interface BlackJackConstant {

    interface Common {
        int PERFECT_POINT = 21;
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
    }
}
