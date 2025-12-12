package com.jjg.game.slots.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.TimeHelper;

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
        //首次玩某个slots游戏，应该使用的模式id
        int FIRST_GAME_GET_MODEL_ID = 4;

        //获取lib失败，总计可尝试次数
        int GET_LIB_FAIL_RETRY_COUNT = 10;

        //单线押分
        int SCORE_TYPE_ONE_BET = 1;
        //总押分
        int SCORE_TYPE_ALL_BET = 2;
        //平均单线押分
        int SCORE_TYPE_AVG_ONE_BET = 3;

        //最大离线时间
        int MAX_OFFLINE_TIME = 5 * TimeHelper.ONE_MINUTE_OF_MILLIS;
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
        //满线图案_x连
        int LINE_TYPE_FULL = 3;
        //全局分散线类型
        int LINE_TYPE_DISPERSE_GLOBAL = 4;
        //满线图案_数量
        int LINE_TYPE_FULL_COUNT = 5;
        //连线_分散 只统计这条线上的图标个数,不论是否相连
        int LINE_TYPE_DISPERSE = 6;
    }

    interface BaseInit{
        //需要走baseline
        int NEED_BASE_LINE = 1;
        //不需要走baseline
        int NOT_NEED_BASE_LINE = 0;
    }

    interface RoomSlotsPool{
        //标准池
        int TYPE_STANDARD = 1;
        //累计添加的保证金
        int TYPE_ALL_REVERSE = 2;
        //累计收益
        int TYPE_ALL_INCOME = 3;
    }

    interface GlobalConfig{
        //创建房间功能基础收益万分比
        int ID_ROOM_INCOME_PROP = 12;
    }
}
