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
        Integer[] SUPPORT_GAME_TYPES = {CoreConst.GameType.DOLLAR_EXPRESS};
    }


    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH;

        BigDecimal BIGDECIMAL_TWO = BigDecimal.valueOf(2);

        //触发局
        int TYPE_TRIGGER = 0;
        //二选一之免费
        int TYPE_FREE = 1;
        //二选一之火车
        int TYPE_TRAIN = 2;

        //首次玩某个slots游戏，应该使用的模式id
        int FIRST_GAME_GET_MODEL_ID = 4;

        //获取lib失败，总计可尝试次数
        int GET_LIB_FAIL_RETRY_COUNT = 5;
    }

    //结果库变更类型
    interface LibChangeType{
        //结果库变更
        int LIB_CHANGE = 0;
        //配置变更
        int CONFIG_CHANGE = 1;
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
    }

    interface GlobalConfig{
        int ID_VIP_OPEN_MAX_LEVEL = 1;
        int ID_BASE_EXP = 2;
        int ID_SWEET = 6;
        int ID_BIG = 7;
        int ID_MEGA = 8;
        int ID_EPIC = 9;
        int ID_LEGENDARY = 10;
    }

    interface BigWinShow{
        int SWEET = 1;
        int BIG = 2;
        int MEGA = 3;
        int EPIC = 4;
        int LEGENDARY = 5;
    }

    interface SpecialAuxiliary{
        //免费旋转
        int TYPE_FREE_ROLL = 0;
        //开启宝箱
        int TYPE_OPEN_BOX = 1;
    }

    interface BaseElementReward{
        //连线类型
        int LINE_TYPE_NORMAL = 1;
        //指定线类型
        int LINE_TYPE_ASSIGN = 2;
        //全局分散线类型
        int LINE_TYPE_DISPERSE_GLOBAL = 4;

        //旋转状态，普通
        int ROTATESTATE_NORMAL = 1;
        //旋转状态，免费
        int ROTATESTATE_FREE = 2;
        //旋转状态，重转
        int ROTATESTATE_AGAIN = 3;
        //旋转状态，全部
        int ROTATESTATE_ALL = 4;

        int REWARD_TYPE_A = 0;
        int REWARD_TYPE_B = 1;
        int REWARD_TYPE_C = 2;
    }

    interface SpecialResultLib{
        //正常
        int TYPE_NORMAL = 1;
        //拉火车
        int TYPE_TRAIN = 2;
        //黄金列车
        int TYPE_GOLD_TRAIN = 3;
        //二选一触发局
        int TYPE_ALL_BOARD = 4;
        //二选一之免费
        int TYPE_ALL_BOARD_FREE = 5;
        //重转之拉火车
        int TYPE_AGAIN_TRAIN = 6;
        //重转之黄金列车
        int TYPE_AGAIN_GOLD_TRAIN = 7;
    }

    interface SpecialGird{
        //随机多种组合元素不同位置
        int GIRD_UPDATE_TYPE_RAND = 0;
        //指定元素替换随机元素位置
        int GIRD_UPDATE_TYPE_APPOINT = 1;
        //指定元素替换随机元素位置
        int GIRD_UPDATE_TYPE_APPOINT_2 = 2;

        //针对普通火车修改格子特殊处理
        int ID_TRAIN_UPDATE = 2;
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
