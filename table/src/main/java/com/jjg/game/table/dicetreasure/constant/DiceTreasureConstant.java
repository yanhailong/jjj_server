package com.jjg.game.table.dicetreasure.constant;

import com.jjg.game.common.constant.MessageConst;

import static com.jjg.game.common.constant.MessageConst.MessageTypeDef.BIRDS_ANIMAL_TYPE;
import static com.jjg.game.common.constant.MessageConst.MessageTypeDef.DICE_BABY_TYPE;

/**
 * 骰宝常量
 *
 * @author 2CL
 */
public interface DiceTreasureConstant {

    int BASE_MSG_PREFIX = DICE_BABY_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface ReqMsgBean {

    }

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_DICE_TREASURE_TABLE_INFO = BASE_MSG_PREFIX | 0x81;
        // 房间结算通知
        int RESP_DICE_TREASURE_SETTLEMENT = BASE_MSG_PREFIX | 0x82;
    }
}
