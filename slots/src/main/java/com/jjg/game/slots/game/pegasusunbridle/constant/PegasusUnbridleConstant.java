package com.jjg.game.slots.game.pegasusunbridle.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface PegasusUnbridleConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.PEGASUS_UNBRIDLE << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_PEGASUS_UNBRIDLE_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_PEGASUS_UNBRIDLE_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_PEGASUS_UNBRIDLE_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_PEGASUS_UNBRIDLE_START_GAME = BASE_MSG_PREFIX | 0x4;
    }


    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        int FU_MA = 1;
    }

    interface SpecialMode {
        int NORMAL = 1;

    }


    interface Common {
        int SPECIAL_PLAY_ID = 5034001;
    }
}
