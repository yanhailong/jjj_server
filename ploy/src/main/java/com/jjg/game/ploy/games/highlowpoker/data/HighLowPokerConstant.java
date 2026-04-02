package com.jjg.game.ploy.games.highlowpoker.data;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2026/3/19
 */
public interface HighLowPokerConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.HIGH_LOW_POKER << MessageConst.MessageCommon.RIGHT_MOVE;
        //进入游戏返回
        int RES_HIGH_LOW_POKER_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        //下注返回
        int RES_HIGH_LOW_POKER_BET = BASE_MSG_PREFIX | 0x2;
        //请求兑换金币
        int REQ_HIGH_LOW_POKER_EXCHANGE = BASE_MSG_PREFIX | 0x3;
        int RES_HIGH_LOW_POKER_EXCHANGE = BASE_MSG_PREFIX | 0x4;
        //请求选择
        int REQ_HIGH_LOW_POKER_CHOOSE = BASE_MSG_PREFIX | 0x5;
        int RES_HIGH_LOW_POKER_CHOOSE = BASE_MSG_PREFIX | 0x6;
    }

    interface Common {
        //最大记录保留数
        int MAX_RECORD_NUM = 20;
        //最多能执行的局数
        int MAX_JOIN_TIMES  = 30;
    }
}
