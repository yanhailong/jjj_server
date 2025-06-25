package com.jjg.game.dollarexpress.constant;

import com.jjg.game.common.constant.MessageConst;

import java.math.BigDecimal;

/**
 * @author 11
 * @date 2025/6/11 14:54
 */
public interface DollarExpressConst {
    interface GameType {
        //美元快递id
        int GAME_TYPE_DOLLAR_EXPRESS = 100100;

        //支持的游戏
        int[] SUPPORT_GAME_TYPES = {GAME_TYPE_DOLLAR_EXPRESS};
    }

    interface Common {
        BigDecimal TEN_THOUSAND = new BigDecimal("10000");

        String AXLE_PREFIX = "axle";
        String SPECIAL_PREFIX = "special";

        String COLUM_PREFIX = "column";
        String ICON_PREFIX = "icon";


        //表示是4*5的游戏，总共会出现20个图标
        int ALL_CION_COUNT = 20;

        //每一列有4个图标
        int COLUM_ICON_COUNT = 4;
        //总共有多少列
        int COLUMS_COUNT = 5;

        //免费模式-火车
        int TRAIN_TYPE_WHEN_WIN_FREE = 1;
        //免费模式-免费次数
        int FREE_TYPE_WHEN_WIN_FREE = 2;

        BigDecimal BIGDECIMAL_TWO = BigDecimal.valueOf(2);
    }

    interface Global {
        int FREE_TYPE_TO_GOLD_TRAIN_COUNT = 1;
    }

    /**
     * resultshow表中的常量
     */
    interface ResultShow {
        //resultshow表中，中普通火车
        int WIN_NORMAL_TRAIN = 1;
        //resultshow表中，中保险箱
        int WIN_SAFE_BOX = 2;
        //resultshow表中，中免费
        int WIN_FREE = 3;
        //resultshow表中，中金火车
        int WIN_GOLD_TRAIN = 4;

        //resultshow表中，绿火车模式
        int GREEN_TRAIN_MOUDLE = 10;
        //resultshow表中，蓝火车模式
        int BLUE_TRAIN_MOUDLE = 11;
        //resultshow表中，紫火车模式
        int PURPLE_TRAIN_MOUDLE = 12;
        //resultshow表中，红火车模式
        int RED_TRAIN_MOUDLE = 13;
        //resultshow表中，金火车模式
        int GOLD_TRAIN_MOUDLE = 14;

        //resultshow表中，免费模式
        int FREE_MOUDLE = 15;
        //resultshow表中，免费模式-中普通火车
        int FREE_TO_WIN_NORMAL_TRAIN_MOUDLE = 16;
        //resultshow表中，免费模式-中金火车
        int FREE_TO_WIN_GOLD_TRAIN_MOUDLE = 17;
        //resultshow表中，投资模式
        int INVEST_MOUDLE = 18;
    }

    /**
     * icon表中的常量
     */
    interface Icon {
        //在icon表中普通图标的类型
        int NORMAL_TYPE = 0;
        //在icon表中WILD图标的类型
        int WILD_TYPE = 1;
        //美金类型
        int DOLLAR_TYPE = 4;

        //红火车id
        int RED_TRAIN_ID = 12;
        //紫火车id
        int PURPLE_TRAIN_ID = 13;
        //蓝火车id
        int BLUE_TRAIN_ID = 14;
        //绿火车id
        int GREEN_TRAIN_ID = 15;

        int ALL_ABOARD_ID = 21;
    }

    interface MsgBean {
        int TYPE = MessageConst.MessageTypeDef.DOLLAR_EXPRESS_TYPE;

        int BASE_MSG_PREFIX = TYPE << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求选择免费模式的游戏
        int REQ_CHOOSE_FREE_MODEL = BASE_MSG_PREFIX | 0x01;
        int RES_CHOOSE_FREE_MODEL = BASE_MSG_PREFIX | 0x02;

        //选择投资地区
        int REQ_INVEST_AREA = BASE_MSG_PREFIX | 0x03;
        int RES_INVEST_AREA = BASE_MSG_PREFIX | 0x03;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x05;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x06;

        //通知配置信息
        int NOTICE_CONFIG_INFO = BASE_MSG_PREFIX | 0x99;
    }
}
