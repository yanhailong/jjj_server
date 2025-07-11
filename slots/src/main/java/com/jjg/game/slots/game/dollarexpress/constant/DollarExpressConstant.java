package com.jjg.game.slots.game.dollarexpress.constant;

import com.jjg.game.common.constant.CoreConst;

/**
 * @author 11
 * @date 2025/7/2 10:28
 */
public interface DollarExpressConstant {
    interface GameType{
        //支持的游戏
        int[] SUPPORT_GAME_TYPES = {CoreConst.GameType.DOLLAR_EXPRESS};
    }

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
    }
}
