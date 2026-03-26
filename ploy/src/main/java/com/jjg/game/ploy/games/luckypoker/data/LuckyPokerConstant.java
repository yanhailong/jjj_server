package com.jjg.game.ploy.games.luckypoker.data;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2026/3/19
 */
public interface LuckyPokerConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.PLOY_LUCKY_POKER << MessageConst.MessageCommon.RIGHT_MOVE;
        //进入游戏返回
        int RES_LUCKY_POKER_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        //下注返回
        int RES_LUCKY_POKER_BET = BASE_MSG_PREFIX | 0x2;

        //请求发牌
        int REQ_DEAL_CARDS = BASE_MSG_PREFIX | 0x3;
        //发牌返回
        int RES_DEAL_CARDS = BASE_MSG_PREFIX | 0x4;
    }
}
