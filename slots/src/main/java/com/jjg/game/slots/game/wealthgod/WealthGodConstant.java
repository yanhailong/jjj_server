package com.jjg.game.slots.game.wealthgod;

import com.jjg.game.common.constant.MessageConst;

/**
 * 财神
 */
public interface WealthGodConstant {

    interface SpecialMode {
        //普通旋转
        int TYPE_NORMAL = 1;
    }

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.WEALTH_GOD << MessageConst.MessageCommon.RIGHT_MOVE;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x2;

        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x3;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x4;

        //请求奖池
        int REQ_POOL_VALUE = BASE_MSG_PREFIX | 0x5;
        int RES_POOL_VALUE = BASE_MSG_PREFIX | 0x6;

    }


}
