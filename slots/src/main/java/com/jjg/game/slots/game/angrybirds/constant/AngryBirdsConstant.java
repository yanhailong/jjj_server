package com.jjg.game.slots.game.angrybirds.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author lm
 * @date 2025/8/1 17:36
 */
public interface AngryBirdsConstant {


    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.ANGRY_BIRDS << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_ANGRY_BIRDS_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_ANGRY_BIRDS_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_ANGRY_BIRDS_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_ANGRY_BIRDS_START_GAME = BASE_MSG_PREFIX | 0x4;
        //获取奖池
        int REQ_ANGRY_BIRDS_POOL_VALUE = BASE_MSG_PREFIX | 0x7;
        int RES_ANGRY_BIRDS_POOL_VALUE = BASE_MSG_PREFIX | 0x8;

    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        //免费
        int FREE = SlotsConst.Status.FREE;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
    }

    interface SpecialPlay {
        //免费游戏倍率配置id
        int FREE_GAME_CONFIG_ID = 5039001;

    }

    interface Common {
        int MINI_POOL_ID = 103900101;
        int MINOR_POOL_ID = 103900102;
        int MAJOR_POOL_ID = 103900103;
        int GRAND_POOL_ID = 103900104;
        //特殊元素替换SpecialAuxiliary表ID
        int SPECIAL_AUXILIARY_ID = 30390004;
        //格子替换的id
        int SPECIAL_GIRD_ID = 10390002;
    }
}
