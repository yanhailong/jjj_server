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
        int GET_LIB_FAIL_RETRY_COUNT = 100;

        //单线押分
        int ONE_LINE = 1;
        //总押分
        int ALL_LINE = 2;
    }

    //结果库变更类型
    interface LibChangeType{
        //结果库变更
        int LIB_CHANGE = 0;
        //配置变更
        int CONFIG_CHANGE = 1;
    }

    interface BaseLine{
        //从左至右
        int DIRECTION_LEFT = 1;
        //从右至左
        int DIRECTION_RIGHT = 2;
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
        //所有
        int LINE_TYPE_ALL = 0;
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
}
