package com.jjg.game.slots.game.dracula;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface DraculaConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.DRACULA_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;

        int REQ_POOL_INFO = BASE_MSG_PREFIX | 0x5;
        int RES_POOL_INFO = BASE_MSG_PREFIX | 0x6;
    }

    interface BaseElement {
        // 银色图标最小 id
        int SILVER_MIN = 55;
        int SILVER_MAX = 87;
        //金色图标最小 id
        int GOLD_MIN = 88;
        //金色图标最大 id
        int GOLD_MAX = 131;
    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        int FREE = SlotsConst.Status.FREE;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
    }

    interface SpecialPlay {
        //连续中奖倍数（主游戏 1/2/3/4/5；免费 3/6/9/12/15）—— 对应 SpecialPlay 表 playType=1
        int TYPE_CONSECUTIVE_WINS = 1;
        //增加免费次数（scatter 增加免费次数）—— 对应 SpecialPlay 表 playType=2
        int TYPE_ADD_FREE_COUNT = 2;
        //百搭赐福（随机 N 个银框符号变 wild）—— 对应 SpecialPlay 表 playType=3
        int TYPE_WILD_BLESSING = 3;
    }

    interface SpecialGird {
        //2格
        int GRID_TWO = 10190005;
        //3格
        int GRID_THERE = 10190006;
        //4格
        int GRID_FOUR = 10190007;
    }
}
