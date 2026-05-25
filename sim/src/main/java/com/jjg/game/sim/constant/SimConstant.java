package com.jjg.game.sim.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2026/5/15
 */
public interface SimConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.SIM_GAME << MessageConst.MessageCommon.RIGHT_MOVE;

        //进入游戏
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //退出游戏
        int REQ_EXIT_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_EXIT_GAME = BASE_MSG_PREFIX | 0x4;

        //完成新手引导
        int REQ_FINISH_GUIDE = BASE_MSG_PREFIX | 0x5;
//        int RES_FINISH_GUIDE = BASE_MSG_PREFIX | 0x6;

        //通知客户端生成游客
        int NOTIFY_GENERATE_GUEST = BASE_MSG_PREFIX | 0x7;

        //请求同步游客到达目的地
        int REQ_SYNC_GUEST_DEST = BASE_MSG_PREFIX | 0x8;
        int RES_SYNC_GUEST_DEST = BASE_MSG_PREFIX | 0x9;

        //获取技能
        int REQ_SIM_GET_SKILLS = BASE_MSG_PREFIX | 0xA;
        int RES_SIM_GET_SKILLS = BASE_MSG_PREFIX | 0xB;

        //升级技能
        int REQ_SIM_UPGRADE_SKILL = BASE_MSG_PREFIX | 0xC;
        int RES_SIM_UPGRADE_SKILL = BASE_MSG_PREFIX | 0xD;
    }

    interface PropConfig {
        //下注金额
        int TYPE_STAKE = 1;
    }

    interface Common {
        //默认赌场 id
        int DEFAULT_CASINO_ID = 1;

        //默认赌场STATS id (CasinoStatsSheetCfg level=0)
        int DEFAULT_CASINO_STATS_ID = 1001;

        //曝光度系数表中分母 (配置中 90 表示 90%)
        int EXPOSURE_COEFFICIENT_BASE = 100;

        //随机模式选目的地时单点最大重试次数
        int MAX_DEST_PICK_RETRY = 10;

        //长时掉线阈值 (ms) — 5 分钟
        long DISCONNECT_LONG_THRESHOLD_MS = 5 * 60 * 1000L;
    }
}
