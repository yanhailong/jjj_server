package com.jjg.game.poker.game.tosouth.constant;

import com.jjg.game.common.constant.MessageConst;

public class ToSouthConstant {

    public interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.TO_SOUTH << MessageConst.MessageCommon.RIGHT_MOVE;
        int REQ_TURN_ACTION = BASE_MSG_PREFIX | 0x1;
        int REQ_CHANGE_TABLE = BASE_MSG_PREFIX | 0x2;
        //响应房间基础信息
        int RESP_ROOM_BASE_INFO = BASE_MSG_PREFIX | 0xA;
        int RESP_SEND_CARDS_INFO = BASE_MSG_PREFIX | 0xB;
        int REPS_CHANG_TABLE = BASE_MSG_PREFIX | 0xC;

        int NOTIFY_SEND_CARD_INFO = BASE_MSG_PREFIX | 0x11;
        int NOTIFY_SETTLEMENT_INFO = BASE_MSG_PREFIX | 0x12;
        int NOTIFY_TURN_ACTION_INFO = BASE_MSG_PREFIX | 0x13;
        int NOTIFY_BOMB_SETTLEMENT = BASE_MSG_PREFIX | 0x14;
    }

    /**
     * 黑桃3的数字
     */
    public static final int RANK_3 = 3;
    public static final int RANK_A = 14;
    public static final int RANK_2 = 15;
    /** 花色值（与PokerPool数据库/EPokerSuit一致）: ♦=1, ♣=2, ♥=3, ♠=4 */
    public static final int DIAMOND_SUIT = 2;
    public static final int CLUB_SUIT = 3;
    public static final int HEART_SUIT = 1;
    public static final int SPADE_SUIT = 4;

    public interface InstantWinType {
        int NONE = 0;
        int FOUR_TWO = 1; // 4个2
        int DRAGON = 2; // 一条龙 (3-A)
        int SAME_COLOR = 3; // 同色
        int SIX_PAIRS = 4; // 6对
        int FIVE_CONSEC_PAIRS = 5; // 5连对
        int SIX_CONSEC_PAIRS = 6; // 6连对
    }
}
