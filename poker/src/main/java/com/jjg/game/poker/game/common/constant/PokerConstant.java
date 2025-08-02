package com.jjg.game.poker.game.common.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author lm
 * @date 2025/7/26 13:57
 */
public interface PokerConstant {
    /**
     * 传入,返回参数类型
     */
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.POKER_GENERAL_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

        //请求更改座位状态
        int REQ_CHANGE_SEAT_STATE = BASE_MSG_PREFIX | 0x1;
        //玩家进行简单牌型操作
        int REQ_SAMPLE_CARD_OPERATION = BASE_MSG_PREFIX | 0x2;
        //通知简单房间操作
        int NOTIFY_SAMPLE_CARD_OPERATION = BASE_MSG_PREFIX | 0x3;

        //请求房间基础信息
        int REQ_ROOM_BASE_INFO = BASE_MSG_PREFIX | 0x4;
        //通知阶段变化
        int NOTIFY_PHASE_CHANGE =  BASE_MSG_PREFIX | 0x5;
        //通知玩家变化
        int NOTIFY_PLAYER_CHANGE =   BASE_MSG_PREFIX | 0x6;
        //通知结算
        int NOTIFY_SETTLEMENT_INFO =  BASE_MSG_PREFIX | 0x7;
    }

    interface PlayerOperation {
        //无操作
        int NONE = 0;
        //弃牌
        int DISCARD = 1;
        //过牌
        int PASS = 2;
        //停牌
        int STOP = 3;
        //加注
        int BET = 4;
        //全压
        int ALL_IN = 5;
    }
}
