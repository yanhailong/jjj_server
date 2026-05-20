package com.jjg.game.slots.game.wolfmoon;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/2/27 15:08
 */
public interface WolfMoonConstant {

    interface SpecialMode {
        //普通旋转
        int TYPE_NORMAL = 1;
        int WILD_MODEL = 2;
        int FREE_CHOOSE = 3;
        //高赔付符号免费游戏
        int FREE_HIGH_PAY = 5;
        //固定堆叠百搭符号免费游戏
        int FREE_FIXED_STACKED_WILD = 6;
        //递增奖励倍数免费游戏
        int FREE_INCREASING_MULTIPLIER = 7;
    }

    interface Status {
        //普通旋转
        int NORMAL = SlotsConst.Status.NORMAL;
        //免费游戏
        int FREE = SlotsConst.Status.FREE;
        //免费游戏选择
        int FREE_CHOOSE = 2;
    }

    interface SpecialPlay {
        //免费模式中倍数增加
        int FREE_MULTIPLE_ADD_ID = 5015001;
        //免费模式中图标增加
        int FREE_ICON_ADD_ID = 5015002;

    }

    /**
     * 图标ID定义
     */
    interface BaseElement {
        // 特殊符号
        // 免费游戏+1
        int EXTRA_FREE = 15;
        //wild符号
        int WILD = 13;
    }

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.WOLF_MOON << MessageConst.MessageCommon.RIGHT_MOVE;

        //请求配置
        int REQ_WOLF_MOON_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_WOLF_MOON_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_WOLF_MOON_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_WOLF_MOON_START_GAME = BASE_MSG_PREFIX | 0x4;

        //（免费游戏选择）
        int REQ_WOLF_MOON_FREE_CHOOSE_ONE = BASE_MSG_PREFIX | 0x5;
        int RES_WOLF_MOON_FREE_CHOOSE_ONE = BASE_MSG_PREFIX | 0x6;

        //获取奖池
        int REQ_WOLF_MOON_POOL_VALUE = BASE_MSG_PREFIX | 0x7;
        int RES_WOLF_MOON_POOL_VALUE = BASE_MSG_PREFIX | 0x8;
    }
}
