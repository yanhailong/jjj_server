package com.jjg.game.slots.game.candyparty.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface CandyPartyConstant {


    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.CANDY_PARTY << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CANDY_PARTY_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_CANDY_PARTY_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_CANDY_PARTY_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_CANDY_PARTY_START_GAME = BASE_MSG_PREFIX | 0x4;
        //获取奖池
        int REQ_CANDY_PARTY_POOL_VALUE = BASE_MSG_PREFIX | 0x7;
        int RES_CANDY_PARTY_POOL_VALUE = BASE_MSG_PREFIX | 0x8;
    }

    interface BaseElement {

    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        //免费
        int FREE = SlotsConst.Status.FREE;

    }

    interface SpecialMode {
        int JACKPOT_MODEL = 3;
        int FREE_MODEL = 2;
        //层数->modelId
        Map<Integer, Integer> FREE_MAP = Map.of(1, 1, 2, 2, 3, 3);
        //层数->modelId
        Map<Integer, Integer> NORMAL_MAP = Map.of(1, 4, 2, 5, 3, 6);
        //层数->modelId
        Map<Integer, Integer> JACKPOT_MAP = Map.of(1, 7, 2, 8, 3, 9);
        //层数->小游戏id
        Map<Integer, Integer> AUXILIARY_MAP = Map.of(1, 30510001, 2, 30510002, 3, 30510003);
    }

    interface SpecialPlay {
        //过关配置
        int PASSING_CRITERIA_ID = 5051001;

    }

    interface Common {
        //modelID->层数

        Map<Integer, Integer> MODEL_ID_LAYER = new HashMap<>();
        int MAX_LAYER = 3;
    }
}
