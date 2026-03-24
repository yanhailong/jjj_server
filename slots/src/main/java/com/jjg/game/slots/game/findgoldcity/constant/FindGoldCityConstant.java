package com.jjg.game.slots.game.findgoldcity.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface FindGoldCityConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.FIND_GOLD_CITY << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_FIND_GOLD_CITY_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_FIND_GOLD_CITY_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_FIND_GOLD_CITY_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_FIND_GOLD_CITY_START_GAME = BASE_MSG_PREFIX | 0x4;

        //获取奖池
        int REQ_FIND_GOLD_CITY_POOL_VALUE = BASE_MSG_PREFIX | 0x7;
        int RES_FIND_GOLD_CITY_POOL_VALUE = BASE_MSG_PREFIX | 0x8;
    }

    interface ElementId {

    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        int FREE = SlotsConst.Status.FREE;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
        int JACKPOT = 3;
    }

    interface Common {
        //连续中奖次数对应的中奖倍率，原倍率上乘以此倍率值
        int BONUS_ACCUMULATION_ID = 5026001;
        //常规模式下，金矿符号转换为百搭符号或黏性百搭符号
        int GOLD_SYMBOL_CONVERSION_ID = 5026003;
    }
}
