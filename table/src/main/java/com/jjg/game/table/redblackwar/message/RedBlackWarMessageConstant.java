package com.jjg.game.table.redblackwar.message;

import com.jjg.game.common.constant.MessageConst;


/**
 * 百家乐消息常量
 *
 * @author 2CL
 */
public interface RedBlackWarMessageConstant {

    int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.RED_BLACK_WAR_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_RED_BLACK_WAR_INFO = BASE_MSG_PREFIX | 0x81;
        int NOTIFY_PLAYER_CHANGE = BASE_MSG_PREFIX | 0x03;
        int NOTIFY_BET_CHANGE = BASE_MSG_PREFIX | 0x04;

    }
}
