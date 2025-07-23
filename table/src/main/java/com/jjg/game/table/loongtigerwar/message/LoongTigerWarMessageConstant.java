package com.jjg.game.table.loongtigerwar.message;

import com.jjg.game.common.constant.MessageConst;


/**
 * 龙虎斗消息常量
 *
 * @author 2CL
 */
public interface LoongTigerWarMessageConstant {

    int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.LOONG_TIGER_WAR_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

    interface RespMsgBean {
        // 进入房间的信息
        int NOTIFY_LOONG_TIGER_WAR_INFO = BASE_MSG_PREFIX | 0x1;
        int NOTIFY_LOONG_TIGER_WAR_SETTLE_INFO = BASE_MSG_PREFIX | 0x02;
    }
}
