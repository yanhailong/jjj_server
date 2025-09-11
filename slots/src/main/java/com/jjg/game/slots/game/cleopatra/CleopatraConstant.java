package com.jjg.game.slots.game.cleopatra;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/8/27 10:56
 */
public interface CleopatraConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.CLEOPATRA << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;
    }

    interface SpecialPlay{
        int TYPE_ADD_ICON = 6;
    }
}
