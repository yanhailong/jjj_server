package com.jjg.game.core.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.sampledata.bean.TaskCfg;

/**
 * 任务相关常量
 */
public interface TaskConstant {

    /**
     * 任务类型 等同于配置TaskType{@link TaskCfg#getTaskType()}
     * TaskType=任务小红点子模块
     */
    interface TaskType {
        /**
         * 积分大奖
         */
        int POINTS_AWARD = 1;
    }

    /**
     * 时间相关常量
     */
    interface TimeConstants {
        /**
         * 中午12点（小时）
         */
        int NOON_HOUR = 12;
        
        /**
         * 无效时间戳阈值
         */
        long INVALID_TIMESTAMP_THRESHOLD = 0L;
        
        /**
         * 全部子模块标识
         */
        int ALL_SUBMODULES = 0;
        
        /**
         * 最小积分奖励阈值
         */
        int MIN_INTEGRAL_REWARD = 0;
        
        /**
         * 单个任务组最小任务数
         */
        int MIN_TASK_GROUP_SIZE = 1;
    }

    /**
     * redis缓存key
     */
    interface RedisKey {
        /**
         * 玩家的任务map
         */
        String TASK_PLAYER_MAP = "task:player:map:";

    }

    /**
     * redis锁key
     */
    interface RedisLockKey {
        /**
         * 玩家的任务map
         */
        String TASK_PLAYER_MAP_LOCK = "taskPlayerMapLock:";

    }

    /**
     * 任务状态
     */
    interface TaskStatus {
        /**
         * 任务状态：进行中
         */
        int STATUS_IN_PROGRESS = 0;
        /**
         * 任务状态：已完成
         */
        int STATUS_COMPLETED = 1;
        /**
         * 任务状态：已领取奖励
         */
        int STATUS_REWARDED = 2;
    }

    /**
     * 任务条件类型
     */
    interface ConditionType {
        /**
         * 单次充值
         */
        int PLAYER_PAY = 11001;

        /**
         * 累计充值
         */
        int PLAYER_SUM_PAY = 11002;

        /**
         * 有效下注
         */
        int PLAYER_BET_ALL = 12001;

        /**
         * 下注次数
         */
        int BET_COUNT = 10001;

        /**
         * 游戏实际赢钱
         */
        int PLAY_GAME_WIN_MONEY = 10003;

        /**
         * 累积使用道具数量
         */
        int PLAY_USE_ITEM = 12101;

    }

    /**
     * 协议
     */
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.TASK_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

        /**
         * 请求任务列表
         */
        int REQ_TASK = BASE_MSG_PREFIX | 0X1;
        int RES_TASK = BASE_MSG_PREFIX | 0X2;

        /**
         * 通知任务更新
         */
        int NOTIFY_TASK_UPDATE = BASE_MSG_PREFIX | 0X3;

        /**
         * 请求任务奖励
         */
        int REQ_TASK_REWARD = BASE_MSG_PREFIX | 0X4;
        /**
         * 响应任务奖励
         */
        int RES_TASK_REWARD = BASE_MSG_PREFIX | 0X5;

    }

}
