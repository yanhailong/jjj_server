package com.jjg.game.table.sizedicetreasure.constant;

import com.jjg.game.common.constant.MessageConst;

import static com.jjg.game.common.constant.MessageConst.MessageTypeDef.SIZE_DICE_BABY_TYPE;

/**
 * 大小骰宝常量
 *
 * @author 2CL
 */
public interface SizeDiceTreasureConstant {

    int BASE_MSG_PREFIX = SIZE_DICE_BABY_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface ReqMsgBean {

    }

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_SIZE_DICE_TABLE_INFO = BASE_MSG_PREFIX | 0x81;
        // 房间结算通知
        int RESP_SIZE_DICE_SETTLEMENT = BASE_MSG_PREFIX | 0x82;
    }
}
