package com.jjg.game.table.vietnamdice.constant;

import com.jjg.game.common.constant.MessageConst;

import static com.jjg.game.common.constant.MessageConst.MessageTypeDef.VIETNAM_SEXY_DISK_TYPE;

/**
 * 越南色碟常量
 *
 * @author 2CL
 */
public interface VietnamDiceConstant {

    int BASE_MSG_PREFIX = VIETNAM_SEXY_DISK_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface ReqMsgBean {

    }

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_ANIMALS_TABLE_INFO = BASE_MSG_PREFIX | 0x81;
        // 房间结算通知
        int RESP_ANIMALS_SETTLEMENT = BASE_MSG_PREFIX | 0x82;
    }
}
