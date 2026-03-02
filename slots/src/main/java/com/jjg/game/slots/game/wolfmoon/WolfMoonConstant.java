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
        //高赔付符号免费游戏
        int FREE_HIGH_PAY = 2;
        //固定堆叠百搭符号免费游戏
        int FREE_FIXED_STACKED_WILD = 3;
        //递增奖励倍数免费游戏
        int FREE_INCREASING_MULTIPLIER = 4;
    }

    interface Status {
        //普通旋转
        int NORMAL = SlotsConst.Status.NORMAL;
        //等待玩家选择免费游戏类型
        int WAITING_FREE_CHOICE = 1;
        //免费游戏中
        int FREE = 2;
    }

    /**
     * 免费游戏类型
     */
    interface FreeGameType {
        //高赔付符号 - 12次
        int HIGH_PAY_SYMBOLS = 1;
        //固定堆叠百搭符号 - 8次
        int FIXED_STACKED_WILD = 2;
        //递增奖励倍数 - 5次，初始倍数5，每次+5，最高100
        int INCREASING_MULTIPLIER = 3;
    }

    /**
     * 免费游戏次数
     */
    interface FreeGameCount {
        int HIGH_PAY_SYMBOLS = 12;
        int FIXED_STACKED_WILD = 8;
        int INCREASING_MULTIPLIER = 5;
    }

    /**
     * 递增倍数配置
     */
    interface Multiplier {
        int INITIAL = 5;
        int INCREMENT = 5;
        int MAX = 100;
    }

    /**
     * 图标ID定义
     */
    interface BaseElement {
        // 特殊符号
        // 免费游戏+1
        int EXTRA_FREE = 15;
    }

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.WOLF_MOON << MessageConst.MessageCommon.RIGHT_MOVE;

        //请求配置
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;

        //二选一（免费游戏选择）
        int REQ_FREE_CHOOSE_ONE = BASE_MSG_PREFIX | 0x5;
        int RES_FREE_CHOOSE_ONE = BASE_MSG_PREFIX | 0x6;

        //获取奖池
        int REQ_POOL_VALUE = BASE_MSG_PREFIX | 0x7;
        int RES_POOL_VALUE = BASE_MSG_PREFIX | 0x8;
    }
}
