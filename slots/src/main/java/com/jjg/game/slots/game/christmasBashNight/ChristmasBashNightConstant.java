package com.jjg.game.slots.game.christmasBashNight;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author lihaocao
 * @date 2025/12/2 17:36
 */
public interface ChristmasBashNightConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.CHRISTMAS_NIGHT_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
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
        int ID_WILD = 11;
        int ID_SCATTER = 14;
        int ID_MINI = 19;
        int ID_MINOR = 20;
        int ID_MAJOR = 21;
        int ID_GRAND = 22;
        //金色图标最小id
        int GOLD_MIN = 11;
        //金色图标最大id
        int GOLD_MAX = 18;
    }


    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
        int JACKPOOL = 3;
    }

    interface SpecialPlay {
        //连续中奖倍数
        int TYPE_CONSECUTIVE_WINS = 4;
        //增加免费次数
        int TYPE_ADD_FREE_COUNT = 5;
    }
}
