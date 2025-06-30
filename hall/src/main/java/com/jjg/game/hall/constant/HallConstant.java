package com.jjg.game.hall.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/6/10 17:04
 */
public interface HallConstant  {

    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH;
    }

    /**
     * 传入,返回参数类型
     */
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.HALL_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

        //登录
        int REQ_LOGIN = BASE_MSG_PREFIX | 0x1;
        int RES_LOGIN = BASE_MSG_PREFIX | 0x2;

        //进入游戏
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x4;

        //选择场次
        int REQ_CHOOSE_WARE = BASE_MSG_PREFIX | 0x5;
        int RES_CHOOSE_WARE = BASE_MSG_PREFIX | 0x6;
    }
}
