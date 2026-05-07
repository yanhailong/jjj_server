package com.jjg.game.slots.game.garaGemstone3;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

public interface GaraGemstone3Constant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.GARA_GEMSTONE_3 << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_GARA_GEMSTONE_3_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_GARA_GEMSTONE_3_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_GARA_GEMSTONE_3_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_GARA_GEMSTONE_3_START_GAME = BASE_MSG_PREFIX | 0x4;

        //请求奖池
        int REQ_GARA_GEMSTONE_3_POOL_INFO = BASE_MSG_PREFIX | 0x5;
        int RES_GARA_GEMSTONE_3_POOL_INFO = BASE_MSG_PREFIX | 0x6;
    }

    interface Status{
        int NORMAL = SlotsConst.Status.NORMAL;
    }

    interface BaseElement{
        int ID_JACKPOOL = 9;
        //下面图标数组 第一个是原图标 第二个是原图标变成两个的图标  第三个是 原图标变成三个的图标
        int[] ID_J = {1,16,24};
        int[] ID_Q = {2,17,25};
        int[] ID_K = {3,18,26};
        int[] ID_A = {4,19,27};
        int[] ID_GREEN = {5,20,28};
        int[] ID_BLUE = {6,21,29};
        int[] ID_RED = {7,22,30};
        int[] ID_WILD = {8,23,31};
    }

    interface SpecialMode{
        int NORMAL = 1;
        int JACKPOOL = 2;
    }

    interface BaseRollerGroup{
        /** 倍数轴滚轴ID，对应 BaseRoller.xlsx 20550214 */
        int MULTIPLY_AXIS_ROLLER_ID = 20550214;
        /** 倍数轴配置ID，对应 SpecialPlay.xlsx 5055023 */
        int MULTIPLY_AXIS_CFG_ID = 5055023;
        /** 基础图标变成多个，对应 SpecialPlay.xlsx 5055021 */
        int EXPAND_MULTIPLY_AXIS_CFG_ID = 5055021;
        /** 基础图标变成多个，对应 SpecialPlay.xlsx 5055022 */
        int EXPAND_NUM_AXIS_CFG_ID = 5055022;
    }
}
