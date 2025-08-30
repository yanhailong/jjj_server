package com.jjg.game.slots.game.dollarexpress;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/7/2 10:28
 */
public interface DollarExpressConstant {
    interface GameType{
        //支持的游戏
        int[] SUPPORT_GAME_TYPES = {CoreConst.GameType.DOLLAR_EXPRESS};
    }

    interface Common{
        int MINI_POOL_ID = 100100101;
        int MINOR_POOL_ID = 100100102;
        int MAJOR_POOL_ID = 100100103;
        int GRAND_POOL_ID = 100100104;
    }

    interface Status{
        int NORMAL = 0;
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

    interface SpecialMode{
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

    interface EventName{
        //自动二选一
        String AUTO_CHOOSE_FREEMODEL_TYPE = "autoChooseFreeModelTypeEvent";
        //自动投资游戏
        String AUTO_INVERS = "autoInvestEvent";
    }

    /**
     * 基础元素
     */
    interface BaseElement{
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
    }

    interface SpecialPlay{
        //随机倍数
        int TYPE_RAND_TIMES = 0;
        //美元现金
        int TYPE_DOLLAR_CASH = 1;
        //投资游戏
        int TYPE_INVERS = 2;
        //黄金列车触发局结果生成倍数
        int TYPE_GOLD_TRAIN = 5;
    }

    interface SpecialGird{
        int ID_COLLECT_DOLLAR = 3;
        int ID_AGAIN_GOLD_TRAIN = 4;
        int ID_NORMAL_GOLD_TRAIN = 5;
    }

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求选择免费模式的游戏
        int REQ_CHOOSE_FREE_MODEL = BASE_MSG_PREFIX | 0x1;
        int RES_CHOOSE_FREE_MODEL = BASE_MSG_PREFIX | 0x2;

        //选择投资地区
        int REQ_INVEST_AREA = BASE_MSG_PREFIX | 0x3;
        int RES_INVEST_AREA = BASE_MSG_PREFIX | 0x4;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x5;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x6;

        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x7;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x8;

        //请求奖池
        int REQ_POOL_VALUE = BASE_MSG_PREFIX | 0x9;
        int RES_POOL_VALUE = BASE_MSG_PREFIX | 0xA;
    }
}
