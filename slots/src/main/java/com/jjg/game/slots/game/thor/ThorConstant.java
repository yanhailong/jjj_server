package com.jjg.game.slots.game.thor;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/12/1 18:14
 */
public interface ThorConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.THOR << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;
    }
}
