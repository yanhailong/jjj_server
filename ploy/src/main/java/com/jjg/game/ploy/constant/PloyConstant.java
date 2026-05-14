package com.jjg.game.ploy.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * 常量
 *
 * @author 11
 * @date 2026/3/19
 */
public interface PloyConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.PLOY_COMMON << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求获取游戏配置
        int REQ_PLOY_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        //请求下注
        int REQ_PLOY_BET = BASE_MSG_PREFIX | 0x2;
        //请求获取记录
        int REQ_PLOY_RECORD = BASE_MSG_PREFIX | 0x3;
    }
}
