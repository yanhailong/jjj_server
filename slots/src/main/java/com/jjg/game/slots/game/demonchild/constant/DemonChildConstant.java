package com.jjg.game.slots.game.demonchild.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface DemonChildConstant {


    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.DEMON_CHILD << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_DEMON_CHILD_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_DEMON_CHILD_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_DEMON_CHILD_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_DEMON_CHILD_START_GAME = BASE_MSG_PREFIX | 0x4;
        //获取奖池
        int REQ_DEMON_CHILD_POOL_VALUE = BASE_MSG_PREFIX | 0x7;
        int RES_DEMON_CHILD_POOL_VALUE = BASE_MSG_PREFIX | 0x8;
    }

    interface BaseElement {
    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        //免费
        int FREE = SlotsConst.Status.FREE;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
    }

    interface SpecialPlay {
    }

}
