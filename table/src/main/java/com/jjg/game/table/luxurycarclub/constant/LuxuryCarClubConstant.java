package com.jjg.game.table.luxurycarclub.constant;

import com.jjg.game.common.constant.MessageConst;

import static com.jjg.game.common.constant.MessageConst.MessageTypeDef.GOOD_CAR_CLUB_TYPE;

/**
 * 豪车俱乐部常量
 *
 * @author 2CL
 */
public interface LuxuryCarClubConstant {

    int BASE_MSG_PREFIX = GOOD_CAR_CLUB_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface ReqMsgBean {

    }

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_LUXURY_CAR_CLUB_TABLE_INFO = BASE_MSG_PREFIX | 0x81;
        // 房间结算通知
        int RESP_LUXURY_CAR_CLUB_SETTLEMENT = BASE_MSG_PREFIX | 0x82;
    }
}
