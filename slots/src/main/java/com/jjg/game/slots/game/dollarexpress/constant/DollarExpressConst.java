package com.jjg.game.slots.game.dollarexpress.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;

import java.math.BigDecimal;

/**
 * @author 11
 * @date 2025/6/27 9:36
 */
public interface DollarExpressConst {
    interface GameType{
        //支持的游戏
        int[] SUPPORT_GAME_TYPES = {CoreConst.GameType.DOLLAR_EXPRESS};
    }

    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH + "dollarexpress";

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

        //第0轴id始终
        int COLUM_0_ID_BEGIN = 0;
        int COLUM_0_ID_END = 3;

        //第1轴id始终
        int COLUM_1_ID_BEGIN = 4;
        int COLUM_1_ID_END = 7;

        //第1轴id始终
        int COLUM_2_ID_BEGIN = 8;
        int COLUM_2_ID_END = 11;

        //第1轴id始终
        int COLUM_3_ID_BEGIN = 12;
        int COLUM_3_ID_END = 15;

        //第1轴id始终
        int COLUM_4_ID_BEGIN = 16;
        int COLUM_4_ID_END = 19;

        //特殊游戏类型
        //拉火车
        int SPECIAL_TYPE_TRAIN = 1;
        //保险箱
        int SPECIAL_TYPE_SAFE_BOX = 2;
        //免费模式
        int SPECIAL_TYPE_FREE = 3;
        //金火车
        int SPECIAL_TYPE_GOLD_TRAIN = 4;

        //在control表，为了计算房间，将特殊中奖id话
        int CONTROLL_SPECIAL_TRAIN_ID = 1001;
        int CONTROLL_SPECIAL_SAFE_BOX_ID = 1002;
        int CONTROLL_SPECIAL_FREE_ID = 1003;
        int CONTROLL_SPECIAL_GOLD_TRAIN_ID = 1004;
    }

    interface Global{
        //免费玩法-触发黄金火车至少需要免费图标数量
        int FREE_TYPE_TO_GOLD_TRAIN_COUNT_ID = 6;
    }

    /**
     * resultshow表中的常量
     */
    interface ResultShow{
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
    interface Icon{
        //在icon表中普通图标的类型
        int NORMAL_TYPE = 0;
        //在icon表中WILD图标的类型
        int WILD_TYPE = 1;
        //美金类型
        int DOLLAR_TYPE = 5;

        //普通图标中最大的id
        int NORMAL_TYPE_MAX_ID = 11;

        //红火车id
        int RED_TRAIN_ID = 12;
        //紫火车id
        int PURPLE_TRAIN_ID = 13;
        //蓝火车id
        int BLUE_TRAIN_ID = 14;
        //绿火车id
        int GREEN_TRAIN_ID = 15;

        //wild图标
        int WILD_1_ID = 16;
        int WILD_2_ID = 17;
        int WILD_5_ID = 18;

        //保险箱
        int SAFE_BOX_ID = 19;

        //黄金火车
        int GOLD_TRAIN_ID = 20;

        int ALL_ABOARD_ID = 21;

        //美元图标
        int DOLLAR_ID = 30;
        //火车图标
        int TRAIN_ID = 31;

        int MINI_ID = 30;
        int MINOR_ID = 30;
        int MAJOR_ID = 30;
        int GRAND_ID = 30;
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
    }
}
