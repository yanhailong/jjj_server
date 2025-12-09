package com.jjg.game.slots.game.thor;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/12/1 18:14
 */
public interface ThorConstant {
    interface SpecialMode{
        //普通旋转
        int TYPE_NORMAL = 1;
        //免费触发局
        int FREE = 2;
        //大奖
        int JACKPOOL = 3;
        //火焰免费
        int FIRE = 4;
        //冰冻免费
        int ICE = 5;
    }

    interface Status{
        //普通旋转
        int NORMAL = 1;
        //免费触发局
        int CHOOSE_ONE = 2;
        //火焰免费
        int FIRE = 3;
        //冰冻免费
        int ICE = 4;
    }

    /**
     * 基础元素
     */
    interface Common{
        //免费模式最后一局修改格子
        int FREE_LAST_ONE_UPDATE_GIRD = 10220099;
    }

    interface BaseElement{
        int ID_WILD = 11;
        int ID_HEIMDALL = 8;
        int ID_SCATTER = 14;
        int ID_MINI = 15;
        int ID_MINOR = 16;
        int ID_MAJOR = 17;
        int ID_GRAND = 18;
    }

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.THOR << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;

        //二选一
        int REQ_FREE_CHOOSE_ONE = BASE_MSG_PREFIX | 0x5;
        int RES_FREE_CHOOSE_ONE = BASE_MSG_PREFIX | 0x6;
    }
}
