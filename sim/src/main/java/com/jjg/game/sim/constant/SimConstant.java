package com.jjg.game.sim.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2026/5/15
 */
public interface SimConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.SIM_GAME << MessageConst.MessageCommon.RIGHT_MOVE;

        //进入游戏
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //退出游戏
        int REQ_EXIT_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_EXIT_GAME = BASE_MSG_PREFIX | 0x4;
    }
}
