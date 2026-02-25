package com.jjg.game.slots.game.hulk;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2026/1/15
 */
public interface HulkConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.HULK << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;

        //请求奖池
        int REQ_POOL_VALUE = BASE_MSG_PREFIX | 0x5;
        int RES_POOL_VALUE = BASE_MSG_PREFIX | 0x6;
    }

    interface SpecialPlay{
        int TYPE_ADD_ICON = 6;
    }

    interface SpecialMode{
        int NORMAL = 1;
    }

    interface BaseElement{

    }
}
