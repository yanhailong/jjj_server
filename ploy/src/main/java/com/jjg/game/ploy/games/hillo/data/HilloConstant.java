package com.jjg.game.ploy.games.hillo.data;

import com.jjg.game.common.constant.MessageConst;

public interface HilloConstant {
    interface MsgBean {
        // HILLO 独立协议号段，所有请求/响应都在 BASE_MSG_PREFIX 下顺序分配。
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.HILLO << MessageConst.MessageCommon.RIGHT_MOVE;
        int RES_HILLO_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_HILLO_BET = BASE_MSG_PREFIX | 0x2;
        int REQ_HILLO_EXCHANGE = BASE_MSG_PREFIX | 0x3;
        int REQ_HILLO_CHOOSE = BASE_MSG_PREFIX | 0x5;
        int RES_HILLO_CHOOSE = BASE_MSG_PREFIX | 0x6;
        int RES_HILLO_RECORD = BASE_MSG_PREFIX | 0x7;
        int REQ_HILLO_SKIP = BASE_MSG_PREFIX | 0x8;
        int REQ_HILLO_AUTO_BET = BASE_MSG_PREFIX | 0xA;
        int RES_HILLO_AUTO_BET_STATUS = BASE_MSG_PREFIX | 0xB;
        int REQ_HILLO_CANCEL_AUTO = BASE_MSG_PREFIX | 0xC;
    }

    interface Common {
        // 记录、猜测次数、跳过次数等玩法限制，和策划文档保持一致。
        int MAX_RECORD_NUM = 100;
        int MAX_JOIN_TIMES = 50;
        int MAX_SKIP_TIMES = 20;
        // 前端发起猜牌请求时传 -1，表示由服务端按自动投注策略推进一步。
        int AUTO_CHOOSE_ID = -1;
        // 万分比，6000 表示 60%。
        int AUTO_WIN_RATE_THRESHOLD = 6000;
    }

    interface BetMode {
        // 当前局下注模式，用于历史记录和限制手动/自动流程互相覆盖。
        int MANUAL = 0;
        int AUTO = 1;
    }

    interface AutoAction {
        // 猜牌响应动作，手动和自动统一按 action 判断本次结果。
        int NONE = 0;
        int START_ROUND = 1;
        int CHOOSE = 2;
        int SKIP = 3;
        int EXCHANGE = 4;
        int ROUND_LOSE = 5;
        int STOP = 6;
    }
}
