package com.jjg.game.slots.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;

import java.math.BigDecimal;

/**
 * @author 11
 * @date 2025/6/27 9:36
 */
public interface SlotsConst {
    interface GameType{
        //支持的游戏
        int[] SUPPORT_GAME_TYPES = {CoreConst.GameType.DOLLAR_EXPRESS};
    }

    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH + "dollarexpress";

        BigDecimal BIGDECIMAL_TWO = BigDecimal.valueOf(2);
    }

    interface BaseElement{
        //普通图标类型
        int TYPE_NORMAL = 0;
        //wild图标
        int TYPE_WILD = 1;
        //分散
        int TYPE_DISPERSE = 2;
        //奖金
        int TYPE_BONUS = 3;
        //分散百搭
        int TYPE_DISPERSE_WILD = 4;

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

    interface DollarSpecialGameType{
        //普通
        int NORMAL = 0;
        //拉火车
        int TRAIN = 1;
        //保险箱
        int SAFE_BOX = 2;
        //免费
        int FREE = 3;
        //金火车
        int GOLD_TRAIN = 4;
    }

    interface SpecialPlay{
        //数字符号指定元素本身每次出现随机的倍数金额
        int TYPE_PLAY_NUMBER_TIMES = 0;
        //美元现金
        int TYPE_PLAY_DOLLAR = 1;
        //投资
        int TYPE_PLAY_INVEST = 2;
        //收集已解锁的地图
        int TYPE_PLAY_MAP = 3;
        //[单线押分值],当前档位低于此值不开启收集功能
        int TYPE_PLAY_MIN_BET_COLLOECT = 4;
    }

    interface BaseElementReward{
        //普通图标类型
        int LINT_TYPE_NORMAL = 1;
        //特殊
        int LINT_TYPE_SPECIAL = 2;
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
