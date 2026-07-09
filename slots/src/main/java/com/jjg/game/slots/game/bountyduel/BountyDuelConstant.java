package com.jjg.game.slots.game.bountyduel;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * 赏金大对决常量。
 */
public interface BountyDuelConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.BOUNTY_DUEL_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;

        int REQ_POOL_INFO = BASE_MSG_PREFIX | 0x5;
        int RES_POOL_INFO = BASE_MSG_PREFIX | 0x6;
    }

    interface BaseElement {
        int GOLD_MIN = 15;
        int GOLD_MAX = 22;
    }

    interface Common {
        /**
         * 单次结果库生成时，免费局总数硬上限。
         */
        int MAX_FREE_GAME_TOTAL = 100;
        /**
         * 单次结果库生成时，免费局嵌套深度硬上限。
         */
        int MAX_FREE_DEEP_TOTAL = 10;
    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        int FREE = SlotsConst.Status.FREE;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
    }

    interface SpecialPlay {
        int TYPE_CONSECUTIVE_WINS = 1;
        int TYPE_ADD_FREE_COUNT = 2;
    }
}
