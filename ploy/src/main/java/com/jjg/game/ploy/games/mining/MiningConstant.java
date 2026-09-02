package com.jjg.game.ploy.games.mining;

import com.jjg.game.common.constant.MessageConst;

public interface MiningConstant {
    int GAME_ID = 400800;
    int BASE = MessageConst.MessageTypeDef.MINIGAME << MessageConst.MessageCommon.RIGHT_MOVE;
    int REQ_INFO = BASE | 0x100;
    int REQ_ACTION = BASE | 0x101;
    int RES_STATE = BASE | 0x102;
    int REQ_RANK = BASE | 0x103;
    int RES_RANK = BASE | 0x104;
    int REQ_EXCHANGE_SHOP = BASE | 0x105;
    int RES_EXCHANGE_SHOP = BASE | 0x106;
    int REQ_BUNDLE_SHOP = BASE | 0x107;
    int RES_BUNDLE_SHOP = BASE | 0x108;
    int REQ_ACHIEVEMENTS = BASE | 0x109;
    int RES_ACHIEVEMENTS = BASE | 0x10A;
    int REQ_DAILY_TASKS = BASE | 0x10B;
    int RES_DAILY_TASKS = BASE | 0x10C;
    int DIG = 1, EXCHANGE = 2, BUNDLE = 3, ACHIEVEMENT = 4, DAILY_TASK = 5;
}
