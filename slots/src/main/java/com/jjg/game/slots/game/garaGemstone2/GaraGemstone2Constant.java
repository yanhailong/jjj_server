package com.jjg.game.slots.game.garaGemstone2;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

public interface GaraGemstone2Constant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.GARA_GEMSTONE_2 << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_GARA_GEMSTONE_2_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_GARA_GEMSTONE_2_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_GARA_GEMSTONE_2_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_GARA_GEMSTONE_2_START_GAME = BASE_MSG_PREFIX | 0x4;

        //请求奖池
        int REQ_GARA_GEMSTONE_2_POOL_INFO = BASE_MSG_PREFIX | 0x5;
        int RES_GARA_GEMSTONE_2_POOL_INFO = BASE_MSG_PREFIX | 0x6;
    }

    interface Status{
        int NORMAL = SlotsConst.Status.NORMAL;
    }

    interface BaseElement{
        int ID_WILD = 8;
        int ID_JACKPOOL = 9;
        int ID_10 = 10;
        int ID_11 = 11;
        int ID_12 = 12;
        int ID_13 = 13;
        int ID_14 = 14;
        int ID_15 = 15;
        int ID_WHEEL = 16;
    }

    interface SpecialMode{
        int NORMAL = 1;
        int JACKPOOL = 2;
        int WHEEL = 3;
    }

    interface BaseRollerGroup{
        /** 倍数轴滚轴ID，对应 BaseRoller.xlsx 20550114 */
        int MULTIPLY_AXIS_ROLLER_ID = 20550114;
        /** 倍数轴配置ID，对应 SpecialPlay.xlsx 5055011 */
        int MULTIPLY_AXIS_CFG_ID = 5055011;
        /** whell模式配置id，对应 SpecialAuxiliary.xlsx 30550101 */
        int WHEEL_SPECIALAUXILIARY_ID =  30550101;
    }
}
