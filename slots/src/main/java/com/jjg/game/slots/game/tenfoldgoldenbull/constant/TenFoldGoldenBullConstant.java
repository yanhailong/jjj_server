package com.jjg.game.slots.game.tenfoldgoldenbull.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface TenFoldGoldenBullConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.TENFOLD_GOLDEN_BULL << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_TEN_FOLD_GOLDEN_BULL_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_TEN_FOLD_GOLDEN_BULL_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_TEN_FOLD_GOLDEN_BULL_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_TEN_FOLD_GOLDEN_BULL_START_GAME = BASE_MSG_PREFIX | 0x4;

        //获取奖池
        int REQ_TEN_FOLD_GOLDEN_BULL_POOL_VALUE = BASE_MSG_PREFIX | 0x7;
        int RES_TEN_FOLD_GOLDEN_BULL_POOL_VALUE = BASE_MSG_PREFIX | 0x8;
    }


    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        int FAKE_LUCKY_BULL = 1;
        int REAL_LUCKY_BULL = 2;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int REAL_LUCKY_BULL = 2;
    }


    interface Common {
        int SPECIAL_PLAY_ID = 5034001;
    }
}
