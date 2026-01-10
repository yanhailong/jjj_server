package com.jjg.game.slots.game.tigerbringsriches.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface TigerBringsRichesConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.TIGER_BRINGS_RICHES << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_TIGER_BRINGS_RICHES_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_TIGER_BRINGS_RICHES_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_TIGER_BRINGS_RICHES_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_TIGER_BRINGS_RICHES_START_GAME = BASE_MSG_PREFIX | 0x4;

        //获取奖池
        int REQ_TIGER_BRINGS_RICHES_POOL_VALUE = BASE_MSG_PREFIX | 0x7;
        int RES_TIGER_BRINGS_RICHES_POOL_VALUE = BASE_MSG_PREFIX | 0x8;
    }

    interface ElementId {
        int BASE_ELEMENT_ID = 1;
        int BLANK = 2;
        int WILD = 3;
    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        int FAKE_TIGER_BRINGS_RICHES = 1;
        int REAL_TIGER_BRINGS_RICHES = 2;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int TIGER_BRINGS_RICHES = 2;
    }

    interface Common {
        int SPECIAL_PLAY_ID = 5034001;
        //随机图标概率
        int RANDOM_ICON_PLAY_ID = 5034002;
        //随机到图标的概率(万分概率)
        int GENERATE_ICON_PLAY_ID = 5034003;
    }
}
