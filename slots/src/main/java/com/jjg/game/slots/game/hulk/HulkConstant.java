package com.jjg.game.slots.game.hulk;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

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

        //汽车小游戏
        int REQ_MINI_CAR = BASE_MSG_PREFIX | 0x7;
        int RES_MINI_CAR = BASE_MSG_PREFIX | 0x8;

        //飞机小游戏
        int REQ_MINI_AIRPLANE = BASE_MSG_PREFIX | 0x9;
        int RES_MINI_AIRPLANE = BASE_MSG_PREFIX | 0xA;
    }

    interface Status {
        //普通旋转
        int NORMAL = SlotsConst.Status.NORMAL;
        //触发免费模式
        int TRIGGER_FREE = SlotsConst.Status.FREE;
        //免费模式
        int FREE = 2;
        //触发小游戏
        int TRIGGER_MINI = 3;
        //触发第3列wild
        int TRIGGER_ONE_WILD = 4;
        //第3列wild
        int ONE_WILD = 5;
        //触发第234列wild
        int TRIGGER_THREE_WILD = 6;
        //第234列wild
        int THREE_WILD = 7;
    }

    interface SpecialAuxiliary {
        //小游戏类型
        int MINI_GAME = 303104;
    }

    interface SpecialPlay {
        int TYPE_ADD_ICON = 6;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
        int MINI = 3;
        int JACKPOT = 4;
        int ONT_WILD = 5;
        int THREE_WILD = 6;
    }

    interface BaseElement {

    }
}
