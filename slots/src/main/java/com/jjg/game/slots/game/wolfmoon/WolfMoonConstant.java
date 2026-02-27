package com.jjg.game.slots.game.wolfmoon;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

public interface WolfMoonConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.WOLF_MOON << MessageConst.MessageCommon.RIGHT_MOVE;

        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;

        int REQ_FREE_CHOOSE_ONE = BASE_MSG_PREFIX | 0x5;
        int RES_FREE_CHOOSE_ONE = BASE_MSG_PREFIX | 0x6;
    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        int CHOOSE_ONE = 1;
        int FREE_HIGH_PAY = 2;
        int FREE_STACK_WILD = 3;
        int FREE_MULTIPLIER = 4;
    }

    interface BaseElement {
        int ID_WILD = 13;
        int ID_SCATTER = 14;
        int ID_ADD_FREE = 15;

        int ID_MINI = 16;
        int ID_MINOR = 17;
        int ID_MAJOR = 18;
        int ID_GRAND = 19;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int WISH_WILD = 2;
        int FREE_TRIGGER = 3;
        int JACKPOT = 4;
        int FREE_HIGH_PAY = 5;
        int FREE_STACK_WILD = 6;
        int FREE_MULTIPLIER = 7;
    }

    interface SpecialPlay {
        int TYPE_FREE_CHOOSE = 1;
        int TYPE_FREE_MULTIPLIER = 2;
        int TYPE_ADD_FREE_COUNT = 3;
    }

    interface FreeChoose {
        int HIGH_PAY = 0;
        int STACK_WILD = 1;
        int MULTIPLIER = 2;
    }
}
