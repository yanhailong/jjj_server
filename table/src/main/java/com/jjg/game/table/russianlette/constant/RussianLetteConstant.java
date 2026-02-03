package com.jjg.game.table.russianlette.constant;

import com.jjg.game.common.constant.MessageConst;

import static com.jjg.game.common.constant.MessageConst.MessageTypeDef.RUSSIAN_ROULETTE_TYPE;

/**
 * 俄罗斯转盘常量
 *
 * @author lhc
 */
public interface RussianLetteConstant {

    int BASE_MSG_PREFIX = RUSSIAN_ROULETTE_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface ReqMsgBean {

    }

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_ANIMALS_TABLE_INFO = BASE_MSG_PREFIX | 0x81;
        // 房间结算通知
        int RESP_ANIMALS_SETTLEMENT = BASE_MSG_PREFIX | 0x82;
    }
}
