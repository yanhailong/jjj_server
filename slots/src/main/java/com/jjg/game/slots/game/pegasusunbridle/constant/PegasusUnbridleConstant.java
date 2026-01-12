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

        //获取奖池
        int REQ_PEGASUS_UNBRIDLE_POOL_VALUE = BASE_MSG_PREFIX | 0x7;
        int RES_PEGASUS_UNBRIDLE_POOL_VALUE = BASE_MSG_PREFIX | 0x8;
    }


    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        int FAKE_FU_MA = 1;
        int REAL_FU_MA = 2;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int REAL_FU_MA = 2;

    }
    interface ElementId {
        int BASE_ELEMENT_ID = 1;
        int BLANK = 9;
        int WILD = 7;
    }

    interface Common {
        int SPECIAL_PLAY_ID = 5034001;
        //随机图标概率
        int RANDOM_ICON_PLAY_ID = 5034002;
        //随机到图标的概率(万分概率)
        int GENERATE_ICON_PLAY_ID = 5034003;
        //元素id_元素滚轴
        int ELEMENT_ROLL = 5034004;
        //奖池id
        int JACKPOT_ID = 103400101;
    }
}
