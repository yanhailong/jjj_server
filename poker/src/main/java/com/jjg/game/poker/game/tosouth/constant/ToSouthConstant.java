package com.jjg.game.poker.game.tosouth.constant;

import com.jjg.game.common.constant.MessageConst;

public class ToSouthConstant {

    public interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.TO_SOUTH << MessageConst.MessageCommon.RIGHT_MOVE;
        int REQ_TURN_ACTION = BASE_MSG_PREFIX | 0x1;

        //响应房间基础信息
        int RESP_ROOM_BASE_INFO = BASE_MSG_PREFIX | 0xA;
        int RESP_SEND_CARDS_INFO = BASE_MSG_PREFIX | 0xB;

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
    public static final int HEART_SUIT = 1;
    public static final int DIAMOND_SUIT = 2;
    public static final int ClUB_SUIT = 3;
    public static final int SPADE_SUITS = 4;

    public interface InstantWinType {
        int NONE = 0;
        int FOUR_TWO = 1; // 4个2
        int DRAGON = 2; // 一条龙 (3-A)
        int SAME_COLOR = 3; // 同色
        int SIX_PAIRS = 4; // 6对
        int FIVE_CONSEC_PAIRS = 5; // 5连对
        int SIX_CONSEC_PAIRS = 6; // 6连对
        int THREE_CONSEC_TRIPLES = 7; // 3 连三张
        int FOUR_CONSEC_TRIPLES = 8; // 4 连三张
        int TWO_QUADS = 9; // 2个四张
        int THREE_QUADS = 10; // 3个四张
        int COMB_BOMB = 11; // 1个四张+3连对
    }
}
