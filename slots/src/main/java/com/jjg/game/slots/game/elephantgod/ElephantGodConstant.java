package com.jjg.game.slots.game.elephantgod;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

public interface ElephantGodConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.ELEPHANT_GOD << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;

        //请求奖池
        int REQ_POOL_INFO = BASE_MSG_PREFIX | 0x5;
        int RES_POOL_INFO = BASE_MSG_PREFIX | 0x6;
    }

    interface Status{
        int NORMAL = SlotsConst.Status.NORMAL;
        //免费模式
        int FREE = SlotsConst.Status.FREE;
    }

    interface BaseElement{
        int ID_WILD = 9;
    }

    interface SpecialMode{
        int NORMAL = 1;
        int FREE = 2;
        int JACKPOOL = 3;
    }

    interface SpecialPlay{
    }
}
