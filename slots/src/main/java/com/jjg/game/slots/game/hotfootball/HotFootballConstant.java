package com.jjg.game.slots.game.hotfootball;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface HotFootballConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.HOT_FOOTBALL_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;
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
        //连续中奖倍数
        int TYPE_CONSECUTIVE_WINS = 4;
        //增加免费次数
        int TYPE_ADD_FREE_COUNT = 5;
    }

    interface SpecialGird {
        //2格
        int GRID_TWO = 10160005;
        //3格
        int GRID_THERE = 10160006;
        //4格
        int GRID_FOUR = 10160007;
    }
}
