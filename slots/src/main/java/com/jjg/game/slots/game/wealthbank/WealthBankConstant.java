package com.jjg.game.slots.game.wealthbank;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

/**
 * @author 11
 * @date 2025/7/2 10:28
 */
public interface WealthBankConstant {
    interface Common {
        int MINI_POOL_ID = 102400101;
        int MINOR_POOL_ID = 102400102;
        int MAJOR_POOL_ID = 102400103;
        int GRAND_POOL_ID = 102400104;
    }

    interface Status {
        int NORMAL = SlotsConst.Status.NORMAL;
        //处于普通二选一
        int NOTMAL_ALL_BOARD = 1;
        //处于黄金列车二选一
        int GOLD_ALL_BOARD = 2;
        //二选一之拉普通火车
        int ALL_BOARD_TRAIN = 3;
        //二选一之拉黄金火车
        int ALL_BOARD_GOLD_TRAIN = 4;
        //二选一之免费模式
        int ALL_BOARD_FREE = 5;
    }

    interface SpecialMode {
        //普通旋转
        int TYPE_NORMAL = 1;
        //拉火车触发局
        int TYPE_TRIGGER_NORMAL_TRAIN = 2;
        //黄金火车触发局
        int TYPE_TRIGGER_GOLD_TRAIN = 3;
        //二选一触发局
        int TYPE_TRIGGER_ALL_BOARD = 4;
        //保险箱触发局
        int TYPE_TRIGGER_SAFE_BOX = 5;
        //免费游戏
        int TYPE_TRIGGER_FREE = 6;
    }


    /**
     * 基础元素
     */
    interface BaseElement {
        //金火车
        int ID_GOLD_TRAIN = 15;
        //保险箱
        int ID_SAFE_BOX = 16;
        //all board
        int ID_ALL_ABOARD = 17;
        //美金
        int ID_DOLLAR = 18;
        //绿火车
        int ID_GREEN_TRAIN = 19;
        //蓝火车
        int ID_BLUE_TRAIN = 20;
        //紫火车
        int ID_PURPLE_TRAIN = 21;
        //红火车
        int ID_RED_TRAIN = 22;
        //美金2
        int ID_DOLLAR_2 = 28;
        //触发免费
        int ID_FREE = 30;
    }

    interface SpecialPlay {
        //美元收集
        int TYPE_COLLECT_DOLLAR = 1;
        //投资3次中奖
        int TYPE_INVERS_ALL_WIN = 2;
        //二选一
        int TYPE_ALL_BOARD = 7;
    }


    interface SpecialAuxiliary {
        //绿火车
        int TYPE_GREEN_TRAIN = 32401;
        //蓝火车
        int TYPE_BLUE_TRAIN = 32402;
        //紫火车
        int TYPE_PUEPLE_TRAIN = 32403;
        //红火车
        int TYPE_RED_TRAIN = 32404;

        //黄金列车
        int TYPE_GOLD_TRAIN = 32421;

        //免费模式-免费旋转
        int TYPE_ALL_BOARD_FREE = 32431;

        //二选一免费次数配置表id
        int FREE_COUNT_CONFIG_ID = 30240007;
    }

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.WEALTH_BANK << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求选择免费模式的游戏
        int REQ_WEALTH_BANK_CHOOSE_FREE_MODEL = BASE_MSG_PREFIX | 0x1;
        int RES_WEALTH_BANK_CHOOSE_FREE_MODEL = BASE_MSG_PREFIX | 0x2;

        //选择投资地区
        int REQ_WEALTH_BANK_INVEST_AREA = BASE_MSG_PREFIX | 0x3;
        int RES_WEALTH_BANK_INVEST_AREA = BASE_MSG_PREFIX | 0x4;

        //开始游戏
        int REQ_WEALTH_BANK_START_GAME = BASE_MSG_PREFIX | 0x5;
        int RES_WEALTH_BANK_START_GAME = BASE_MSG_PREFIX | 0x6;

        //请求配置
        int REQ_WEALTH_BANK_CONFIG_INFO = BASE_MSG_PREFIX | 0x7;
        int RES_WEALTH_BANK_CONFIG_INFO = BASE_MSG_PREFIX | 0x8;

        //请求奖池
        int REQ_WEALTH_BANK_POOL_VALUE = BASE_MSG_PREFIX | 0x9;
        int RES_WEALTH_BANK_POOL_VALUE = BASE_MSG_PREFIX | 0xA;
    }
}
