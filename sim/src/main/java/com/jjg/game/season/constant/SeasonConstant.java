package com.jjg.game.season.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2026/7/9
 */
public interface SeasonConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.SEASON << MessageConst.MessageCommon.RIGHT_MOVE;

        //赛季玩法
        int REQ_SEASON_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_SEASON_INFO = BASE_MSG_PREFIX | 0x2;

        int REQ_SEASON_SHOP = BASE_MSG_PREFIX | 0x3;
        int RES_SEASON_SHOP = BASE_MSG_PREFIX | 0x4;

        int REQ_SEASON_BUY = BASE_MSG_PREFIX | 0x5;
        int RES_SEASON_BUY = BASE_MSG_PREFIX | 0x6;

        int REQ_SEASON_GEMS = BASE_MSG_PREFIX | 0x7;
        int RES_SEASON_GEMS = BASE_MSG_PREFIX | 0x8;

        int REQ_SEASON_EQUIP_GEM = BASE_MSG_PREFIX | 0x9;
        int RES_SEASON_EQUIP_GEM = BASE_MSG_PREFIX | 0xA;

        int REQ_SEASON_CRAFT_GEM = BASE_MSG_PREFIX | 0xB;
        int RES_SEASON_CRAFT_GEM = BASE_MSG_PREFIX | 0xC;

        int REQ_SEASON_MATCH = BASE_MSG_PREFIX | 0xD;
        int RES_SEASON_MATCH = BASE_MSG_PREFIX | 0xE;

        int NOTIFY_SEASON_MATCH_RESULT = BASE_MSG_PREFIX | 0xF;

        int REQ_SEASON_MATCH_HISTORY = BASE_MSG_PREFIX | 0x11;
        int RES_SEASON_MATCH_HISTORY = BASE_MSG_PREFIX | 0x12;

        int REQ_SEASON_RANK = BASE_MSG_PREFIX | 0x13;
        int RES_SEASON_RANK = BASE_MSG_PREFIX | 0x14;

        //试炼任务 (新手赛季关卡)
        int REQ_SEASON_TRIALS = BASE_MSG_PREFIX | 0x15;
        int RES_SEASON_TRIALS = BASE_MSG_PREFIX | 0x16;

        int REQ_SEASON_TRIAL_CHALLENGE = BASE_MSG_PREFIX | 0x17;
        int RES_SEASON_TRIAL_CHALLENGE = BASE_MSG_PREFIX | 0x18;

        int NOTIFY_SEASON_TRIAL_RESULT = BASE_MSG_PREFIX | 0x19;

        int REQ_SEASON_TRIAL_PROGRESS = BASE_MSG_PREFIX | 0x1B;
        int RES_SEASON_TRIAL_PROGRESS = BASE_MSG_PREFIX | 0x1C;

        int REQ_SEASON_TIER_UP_REWARDS = BASE_MSG_PREFIX | 0x1D;
        int RES_SEASON_TIER_UP_REWARDS = BASE_MSG_PREFIX | 0x1E;

        int REQ_SEASON_TIER_SETTLEMENT_REWARDS = BASE_MSG_PREFIX | 0x1F;
        int RES_SEASON_TIER_SETTLEMENT_REWARDS = BASE_MSG_PREFIX | 0x20;
    }
}
