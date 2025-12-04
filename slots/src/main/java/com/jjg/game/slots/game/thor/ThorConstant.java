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
    interface BaseElement{
        //金火车
        int ID_GOLD_TRAIN = 15;
        //保险箱
        int ID_SAFE_BOX = 16;
        //all board
        int ID_ALL_ABOARD = 17;
        //美金
        int ID_DOLLAR = 18;
        //绿火车
        int ID_GREEN_TRAIN = 19;
        //蓝火车
        int ID_BLUE_TRAIN = 20;
        //紫火车
        int ID_PURPLE_TRAIN = 21;
        //红火车
        int ID_RED_TRAIN = 22;
        //美金2
        int ID_DOLLAR_2 = 28;
        //触发免费
        int ID_FREE = 30;
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
