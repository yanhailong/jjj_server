package com.jjg.game.ploy.games.airraid.data;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2026/3/27
 */
public interface AirRaidConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.PLOY_AIR_RAID << MessageConst.MessageCommon.RIGHT_MOVE;
        //进入游戏返回
        int RES_AIR_RAID_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        //下注返回
        int RES_AIR_RAID_BET = BASE_MSG_PREFIX | 0x2;
        //兑现请求
        int REQ_AIR_RAID_CASH_OUT = BASE_MSG_PREFIX | 0x3;
        //兑现响应
        int RES_AIR_RAID_CASH_OUT = BASE_MSG_PREFIX | 0x4;
        //游戏状态广播
        int RES_AIR_RAID_GAME_STATE = BASE_MSG_PREFIX | 0x5;
        //坠毁广播
        int RES_AIR_RAID_CRASH = BASE_MSG_PREFIX | 0x6;


        //游戏状态同步
        int GAME_STATE_SYNC = BASE_MSG_PREFIX | 0x91;
        //下注同步
        int BET_SYNC = BASE_MSG_PREFIX | 0x92;
        //兑现同步
        int CASH_OUT_SYNC = BASE_MSG_PREFIX | 0x93;
        //坠毁同步
        int CRASH_SYNC = BASE_MSG_PREFIX | 0x94;
    }

    interface Common {
        //下注阶段时间(真正下注)
        int BET_PHASE_TIME_MILLS = 5000;
        //下注阶段最后的时间停止下注
        int BET_PHASE_TIME_BEFORE_END_MILLS = 2000;
        //结算阶段
        int CRASHED_PHASE_TIME_MILLS = 3000;
    }
}
