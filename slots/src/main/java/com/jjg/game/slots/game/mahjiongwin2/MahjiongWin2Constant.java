package com.jjg.game.slots.game.mahjiongwin2;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface MahjiongWin2Constant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.MAHJIONG_WIN2_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;
    }

    interface BaseElement {
        //金色图标最小id
        int GOLD_MIN = 12;
        //金色图标最大id
        int GOLD_MAX = 20;
    }

    interface Common {
        //单次结果库生成中，免费局总数的硬上限。防止嵌套触发免费时无限膨胀导致内存溢出
        int MAX_FREE_GAME_TOTAL = 100;
        //单次结果库生成中，免费中免费的总层数。防止嵌套触发免费时无限膨胀导致内存溢出
        int MAX_FREE_DEEP_TOTAL = 10;
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
        //连续中奖倍数
        int TYPE_CONSECUTIVE_WINS = 4;
        //增加免费次数
        int TYPE_ADD_FREE_COUNT = 5;

    }
}
