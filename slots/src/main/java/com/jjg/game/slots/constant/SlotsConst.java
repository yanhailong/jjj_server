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

    interface BigWinShow{
        int SWEET = 1;
        int BIG = 2;
        int MEGA = 3;
        int EPIC = 4;
        int LEGENDARY = 5;
    }

    interface BaseElementReward{
        //所有
        int LINE_TYPE_ALL = 0;
        //连线类型
        int LINE_TYPE_NORMAL = 1;
        //指定线类型
        int LINE_TYPE_ASSIGN = 2;
        //满线图案
        int LINE_TYPE_FULL = 3;
        //全局分散线类型
        int LINE_TYPE_DISPERSE_GLOBAL = 4;
    }

    interface BaseInit{
        //需要走baseline
        int NEED_BASE_LINE = 1;
        //不需要走baseline
        int NOT_NEED_BASE_LINE = 0;
    }
}
