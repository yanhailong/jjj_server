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
        int NOTIFY_AIR_RAID_GAME_STATE = BASE_MSG_PREFIX | 0x5;
        //兑现批量广播(每秒累积推送)
        int NOTIFY_AIR_RAID_CASH_OUT = BASE_MSG_PREFIX | 0x6;

        //自动兑现请求
        int REQ_AIR_RAID_AUTO_CASH_OUT = BASE_MSG_PREFIX | 0x7;
        //自动兑现响应
        int RES_AIR_RAID_AUTO_CASH_OUT = BASE_MSG_PREFIX | 0x8;

        //获取个人历史记录
        int RES_AIR_RAID_RECORD = BASE_MSG_PREFIX | 0x9;

        int NOTIFY_AIR_RAID_BET = BASE_MSG_PREFIX | 0xA;
        //排行榜请求
        int REQ_AIR_RAID_RANK = BASE_MSG_PREFIX | 0xB;
        //排行榜响应
        int RES_AIR_RAID_RANK = BASE_MSG_PREFIX | 0xC;

        //获取上一回合的信息
        int REQ_AIR_RAID_LAST_ROUND = BASE_MSG_PREFIX | 0xD;
        //获取上一回合的信息
        int RES_AIR_RAID_LAST_ROUND = BASE_MSG_PREFIX | 0xE;

        //游戏状态同步
        int GAME_STATE_SYNC = BASE_MSG_PREFIX | 0x91;
        //下注同步
        int BET_SYNC = BASE_MSG_PREFIX | 0x92;
        //兑现同步
        int CASH_OUT_SYNC = BASE_MSG_PREFIX | 0x93;
    }

    interface Common {
        int AIR_BIR_REWARD = 50000;
    }

    interface Odds {
        //倍数增长率
        int GROWTH = 0;
        //风险增量
        int RISK = 1;
        //初始坠毁率
        int CRASH = 2;
    }
}
