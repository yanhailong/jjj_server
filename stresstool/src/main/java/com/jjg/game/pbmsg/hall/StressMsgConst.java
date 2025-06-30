package com.jjg.game.pbmsg.hall;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 2CL
 */
public interface StressMsgConst {
    /**
     * 传入,返回参数类型
     */
    interface HallMsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.HALL_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

        //登录
        int REQ_LOGIN = BASE_MSG_PREFIX | 0x01;
        int RES_LOGIN = BASE_MSG_PREFIX | 0x02;

        //进入游戏
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x03;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x04;

        //选择场次
        int REQ_CHOOSE_WARE = BASE_MSG_PREFIX | 0x05;
        int RES_CHOOSE_WARE = BASE_MSG_PREFIX | 0x06;
    }
}
