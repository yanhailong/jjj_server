package com.jjg.game.slots.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.utils.TimeHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2025/6/27 9:36
 */
public class SlotsConst {
    //jackpot的类型id
    public static final Map<Integer, Set<Integer>> specialModeJackpotModeIds = new HashMap<>();
    //免费触发局的类型id
    public static final Map<Integer, Set<Integer>> specialModeTriggerFreeModeIds = new HashMap<>();

    static {
        //jackpot的类型id
        specialModeJackpotModeIds.put(CoreConst.GameType.SUPER_STAR, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.WEALTH_GOD, Set.of(2));
        specialModeJackpotModeIds.put(CoreConst.GameType.CLEOPATRA, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.CHRISTMAS_PARTY, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.THOR, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.BASKETBALL_STAR, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.FROZEN_THRONE, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.PAN_JIN_LIAN, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.STEAM_AGE, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.GOLD_SNAKE_FORTUNE, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.PEGASUS_UNBRIDLE, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.CAPTAIN_JACK, Set.of(4));
        specialModeJackpotModeIds.put(CoreConst.GameType.MONEY_RABBIT, Set.of(3));
        specialModeJackpotModeIds.put(CoreConst.GameType.DEMON_CHILD, Set.of(3));

        //免费触发局的类型id
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.DOLLAR_EXPRESS, Set.of(6));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.MAHJIONG_WIN, Set.of(2));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.WEALTH_BANK, Set.of(6));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.CHRISTMAS_PARTY, Set.of(2));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.THOR, Set.of(2));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.BASKETBALL_STAR, Set.of(2));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.FROZEN_THRONE, Set.of(2));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.PAN_JIN_LIAN, Set.of(2));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.STEAM_AGE, Set.of(2));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.GOLD_SNAKE_FORTUNE, Set.of(2));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.CAPTAIN_JACK, Set.of(2));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.MONEY_RABBIT, Set.of(2));
        specialModeTriggerFreeModeIds.put(CoreConst.GameType.DEMON_CHILD, Set.of(2));
    }

    public class Status {
        public static final int NORMAL = 0;
        //免费
        public static final int FREE = 1;
    }

    public class Common {
        //首次玩某个slots游戏，应该使用的模式id
        public static final int FIRST_GAME_GET_MODEL_ID = 4;

        //获取lib失败，总计可尝试次数
        public static final int GET_LIB_FAIL_RETRY_COUNT = 10;

        //单线押分
        public static final int SCORE_TYPE_ONE_BET = 1;
        //总押分
        public static final int SCORE_TYPE_ALL_BET = 2;
        //平均单线押分
        public static final int SCORE_TYPE_AVG_ONE_BET = 3;

        //最大离线时间
        public static final int MAX_OFFLINE_TIME = 5 * TimeHelper.ONE_MINUTE_OF_MILLIS;

        //无效的图标起始id
        public static final int INVALID_ICON_BEGIN_ID = 1000;

        //不可变固定元素
        public static final int IMMUTABLE_ELEMENTS = 99999;

    }

    //结果库变更类型
    public class LibChangeType {
        //结果库变更
        public static final int LIB_CHANGE = 0;
        //配置变更
        public static final int CONFIG_CHANGE = 1;
    }

    public class BaseLine {
        //从左至右
        public static final int DIRECTION_LEFT = 1;
        //从右至左
        public static final int DIRECTION_RIGHT = 2;
    }

    public class BaseElement {
        //普通图标类型
        public static final int TYPE_NORMAL = 0;
        //wild图标
        public static final int TYPE_WILD = 1;
        //分散
        public static final int TYPE_DISPERSE = 2;
        //奖金
        public static final int TYPE_BONUS = 3;
        //分散百搭
        public static final int TYPE_DISPERSE_WILD = 4;
    }

    public class BigWinShow {
        public static final int SWEET = 1;
        public static final int BIG = 2;
        public static final int MEGA = 3;
        public static final int EPIC = 4;
        public static final int LEGENDARY = 5;
    }

    public class BaseElementReward {
        //所有
        public static final int LINE_TYPE_ALL = 0;
        //连线类型
        public static final int LINE_TYPE_NORMAL = 1;
        //指定线类型
        public static final int LINE_TYPE_ASSIGN = 2;
        //满线图案_x连
        public static final int LINE_TYPE_FULL = 3;
        //全局分散线类型
        public static final int LINE_TYPE_DISPERSE_GLOBAL = 4;
        //满线图案_数量
        public static final int LINE_TYPE_FULL_COUNT = 5;
        //连线_分散 只统计这条线上的图标个数,不论是否相连
        public static final int LINE_TYPE_DISPERSE = 6;
    }

    public class BaseInit {
        //需要走baseline
        public static final int NEED_BASE_LINE = 1;
        //不需要走baseline
        public static final int NOT_NEED_BASE_LINE = 0;
    }

    public class GlobalConfig {
        //创建房间功能基础收益万分比
        public static final int ID_ROOM_INCOME_PROP = 12;
        //创建房间-Slot游戏新增超时提出的倒计时
        public static final int ID_ROOM_PLAYER_ILDE_TIME_MILLS = 112;
    }


    public class SlotsCommon {
        public static final int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.SLOTS_COMMON << MessageConst.MessageCommon.RIGHT_MOVE;

        //在slots好友房获取准备金
        public static final int REQ_SLOTS_ROOM_POOL = BASE_MSG_PREFIX | 0x1;
        public static final int RES_SLOTS_ROOM_POOL = BASE_MSG_PREFIX | 0x2;

        //获取游戏状态
        public static final int REQ_SLOTS_STATUS = BASE_MSG_PREFIX | 0x3;
        public static final int RES_SLOTS_STATUS = BASE_MSG_PREFIX | 0x4;
    }
}
