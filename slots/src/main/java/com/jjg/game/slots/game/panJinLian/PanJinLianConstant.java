package com.jjg.game.slots.game.panJinLian;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author codex
 * @date 2026/2/25
 */
public interface PanJinLianConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.PAN_JIN_LIAN << MessageConst.MessageCommon.RIGHT_MOVE;
        // request config
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        // start game
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;

        // request pool
        int REQ_POOL_INFO = BASE_MSG_PREFIX | 0x5;
        int RES_POOL_INFO = BASE_MSG_PREFIX | 0x6;
    }

    interface Common {
        int MINI_POOL_ID = 103900101;
        int MINOR_POOL_ID = 103900102;
        int MAJOR_POOL_ID = 103900103;
        int GRAND_POOL_ID = 103900104;
    }

    interface Status {
        int NORMAL = 0;
        int FREE = 1;
    }

    interface BaseElement {
        int ID_WILD = 10;
        int ID_SCATTER = 11;
        int ID_MINI = 12;
        int ID_MINOR = 13;
        int ID_MAJOR = 14;
        int ID_GRAND = 15;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
        int JACKPOOL = 3;
    }

    interface SpecialPlay {
        // add free spin count
        int TYPE_ADD_FREE_COUNT = 5;
    }
}
