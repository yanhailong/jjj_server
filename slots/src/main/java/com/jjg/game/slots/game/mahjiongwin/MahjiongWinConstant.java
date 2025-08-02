package com.jjg.game.slots.game.mahjiongwin;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface MahjiongWinConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.MAHJIONG_WIN_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;
    }
}
