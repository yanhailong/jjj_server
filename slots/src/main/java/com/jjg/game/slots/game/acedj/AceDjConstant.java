package com.jjg.game.slots.game.acedj;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author lihaocao
 * @date 2025/12/2 17:36
 */
public interface AceDjConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.ACE_DJ << MessageConst.MessageCommon.RIGHT_MOVE;
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

    interface Common {
        int MINI_POOL_ID = 101700101;
        int MINOR_POOL_ID = 101700102;
        int MAJOR_POOL_ID = 101700103;
        int GRAND_POOL_ID = 101700104;
    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        int FREE = SlotsConst.Status.FREE;
    }
    interface BaseElement{
        int[] ID_WILD_ARR = {12,29,41,53};
        int ID_SCATTER = 13;
        int ID_MINI = 14;
        int ID_MINOR = 15;
        int ID_MAJOR = 16;
        int ID_GRAND = 17;
        //金色图标最小id
        int GOLD_MIN = 54;
        //金色图标最大id
        int GOLD_MAX = 86;
        //空图标（大图标占用）
        int ID_NULL = 0;
    }


    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
        int JACKPOOL = 3;
    }

    interface SpecialPlay {
        //连续中奖倍数
        int TYPE_CONSECUTIVE_WINS = 2;
        //增加免费次数
        int TYPE_ADD_FREE_COUNT = 1;
    }

    interface SpecialGird {
        //2格
        int GRID_TWO = 10280006;
        //3格
        int GRID_THERE = 10280007;
        //4格
        int GRID_FOUR = 10280008;
    }
}
