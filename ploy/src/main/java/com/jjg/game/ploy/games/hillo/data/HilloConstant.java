package com.jjg.game.ploy.games.hillo.data;

import com.jjg.game.common.constant.MessageConst;

public interface HilloConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.HILLO << MessageConst.MessageCommon.RIGHT_MOVE;
        int RES_HILLO_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_HILLO_BET = BASE_MSG_PREFIX | 0x2;
        int REQ_HILLO_EXCHANGE = BASE_MSG_PREFIX | 0x3;
        int RES_HILLO_EXCHANGE = BASE_MSG_PREFIX | 0x4;
        int REQ_HILLO_CHOOSE = BASE_MSG_PREFIX | 0x5;
        int RES_HILLO_CHOOSE = BASE_MSG_PREFIX | 0x6;
        int RES_HILLO_RECORD = BASE_MSG_PREFIX | 0x7;
        int REQ_HILLO_SKIP = BASE_MSG_PREFIX | 0x8;
        int RES_HILLO_SKIP = BASE_MSG_PREFIX | 0x9;
        int REQ_HILLO_AUTO_BET = BASE_MSG_PREFIX | 0xA;
        int RES_HILLO_AUTO_BET = BASE_MSG_PREFIX | 0xB;
        int REQ_HILLO_CANCEL_AUTO = BASE_MSG_PREFIX | 0xC;
    }

    interface Common {
        int MAX_RECORD_NUM = 100;
        int MAX_JOIN_TIMES = 50;
        int MAX_SKIP_TIMES = 20;
        int AUTO_INTERVAL_MS = 700;
        int AUTO_WIN_RATE_THRESHOLD = 6000;
    }

    interface BetMode {
        int MANUAL = 0;
        int AUTO = 1;
    }

    interface AutoAction {
        int NONE = 0;
        int START_ROUND = 1;
        int CHOOSE = 2;
        int SKIP = 3;
        int EXCHANGE = 4;
        int ROUND_LOSE = 5;
        int STOP = 6;
    }
}
