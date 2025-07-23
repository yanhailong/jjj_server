package com.jjg.game.table.redblackwar.message;

import com.jjg.game.common.constant.MessageConst;


/**
 * 红黑大战消息常量
 *
 * @author 2CL
 */
public interface RedBlackWarMessageConstant {

    int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.RED_BLACK_WAR_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_RED_BLACK_WAR_INFO = BASE_MSG_PREFIX | 0x81;
        int NOTIFY_RED_BLACK_WAR_SETTLE_INFO = BASE_MSG_PREFIX | 0x03;

    }
}
