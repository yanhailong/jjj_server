package com.jjg.game.table.riveranimals.constant;

import com.jjg.game.common.constant.MessageConst;

import static com.jjg.game.common.constant.MessageConst.MessageTypeDef.FISH_SHRIMP_CRAB_TYPE;

/**
 * 鱼虾蟹常量
 *
 * @author 2CL
 */
public interface RiverAnimalsConstant {

    int BASE_MSG_PREFIX = FISH_SHRIMP_CRAB_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface ReqMsgBean {

    }

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_ANIMALS_TABLE_INFO = BASE_MSG_PREFIX | 0x81;
        // 房间结算通知
        int RESP_ANIMALS_SETTLEMENT = BASE_MSG_PREFIX | 0x82;
    }
}
