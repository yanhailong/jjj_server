package com.jjg.game.hall.pointsaward.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * 积分大奖常量
 */
public interface PointsAwardConstant {

    /**
     * 缓存key
     */
    interface RedisKey {
        /**
         * 玩家积分
         */
        String POINTS_AWARD_DATA_POINTS = "pointsAwardDataPoints:";
        /**
         * 签到次数
         */
        String POINTS_AWARD_SING_IN_COUNT = "pointsAwardSingIn:count:";
        /**
         * 最后一次签到时间
         */
        String POINTS_AWARD_SING_IN_DATE = "pointsAwardSingIn:date:";
        /**
         * 积分大奖排行榜
         */
        String POINTS_AWARD_RANKING = "pointsAwardRanking:";
        /**
         * 转盘历史记录
         */
        String POINTS_AWARD_TURNTABLE_HISTORY = "pointsAwardTurntableHistory:";
        /**
         * 转盘当前最大次数
         */
        String TURNTABLE_COUNT = "turntableCount:";
        /**
         * 转盘增加的次数
         */
        String TURNTABLE_ADD_COUNT = "turntableAddCount:";
        /**
         * 玩家的排行榜历史记录
         */
        String POINTS_AWARD_PLAYER_RANKING_HISTORY = "pointsAwardPlayerRankingHistory:";
        /**
         * 所有排行榜的历史记录
         */
        String POINTS_AWARD_RANKING_HISTORY = "pointsAwardRankingHistory:";
        /**
         * 积分大奖积分阶梯奖励领取记录
         */
        String POINTS_AWARD_LADDER_REWARDS_RECEIVE = "pointsAwardLadderRewardsReceive:";
        /**
         * 玩家积分大奖累计充值金额记录
         */
        String POINTS_AWARD_RECHARGE = "pointsAwardRecharge:";
    }

    /**
     * 锁
     */
    interface RedisLockKey {
        /**
         * 玩家积分大奖数据操作锁
         */
        String POINTS_AWARD_DATA_LOCK = "pointsAwardDataLock:";
        /**
         * 玩家签到锁
         */
        String POINTS_AWARD_SING_IN_LOCK = "pointsAwardSignInLock:";
        /**
         * 积分大奖排行榜更新锁
         */
        String POINTS_AWARD_RANKING_LOCK = "pointsAwardRankingLock:";
        /**
         * 玩家积分大奖数据初始化锁
         */
        String POINTS_AWARD_DATA_LOCK_TURNTABLE_INIT = "pointsAwardDataLock:turntable:init";
        /**
         * 玩家排行榜奖励和历史记录锁
         */
        String PLAYER_RANKING_AWARD_LOCK = "playerRankingAwardLock:";
    }

    /**
     * 排行榜相关常量
     */
    interface Leaderboard {

        /**
         * 奖励类型
         */
        interface AwardType {
            /**
             * 其他
             */
            int OTHER = 1;
            /**
             * 道具
             */
            int ITEM = 2;
        }

        /**
         * 上午榜类型值
         */
        int AM = 1;
        /**
         * 下午榜类型值
         */
        int PM = 2;
        /**
         * 月榜类型值（与 AM/PM 区分开，避免混淆）
         */
        int TYPE_MONTH = 3;
        /**
         * 排行榜更新锁持有时间（毫秒）
         */
        int LOCK_LEASE_MILLIS = 1000;
        /**
         * 时间基准（用于小数偏移）
         */
        long TIME_BASE_MS = 9_999_999_999_999L;
        /**
         * 时间偏移缩放，确保偏移 < 1，不影响主要积分排序
         */
        double EPSILON_DIVISOR = 100_000_000_000_000d;
        /**
         * 排行榜最多保留的名次
         */
        int MAX_RANK_SIZE = 50;
        /**
         * 上午榜时段名字
         */
        String RANK_NAME_AM = "00:00:00-12:00:00";
        /**
         * 下午榜时段名字
         */
        String RANK_NAME_PM = "12:00-24:00";
        /**
         * 玩家最大历史记录保留条数
         */
        int PLAYER_MAX_HISTORY_SIZE = 100;
        /**
         * 排行榜最大历史记录
         */
        int MAX_HISTORY_SIZE = 100;
    }

    /**
     * 锁持有时间
     */
    interface WaitTime {
        /**
         * 玩家积分数据写锁持有时间（毫秒）
         */
        int LOCK_LEASE_MILLIS = 2000;
    }

    /**
     * 转盘相关
     */
    interface Turntable {
        /**
         * 转盘历史记录最大条数  策划要求写死
         */
        int HISTORY_MAX_SIZE = 50;
    }

    /**
     * 红点子模块
     */
    interface RedDotSubModule {
        /**
         * 签到
         */
        int SIGN_IN = 1;

    }

    /**
     * 协议
     */
    interface Message {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.POINTS_AWARD << MessageConst.MessageCommon.RIGHT_MOVE;

        //请求签到配置
        int REQ_SIGN_CONFIG = BASE_MSG_PREFIX | 0x01;
        //签到配置回返
        int RES_SIGN_CONFIG = BASE_MSG_PREFIX | 0x02;

        //请求签到
        int REQ_SIGN = BASE_MSG_PREFIX | 0x03;
        //请求签到结果回返
        int RES_SIGN = BASE_MSG_PREFIX | 0x04;

        //请求转盘数据
        int REQ_TURNTABLE_CONFIG = BASE_MSG_PREFIX | 0x05;
        //请求转盘数据回复
        int RES_TURNTABLE_CONFIG = BASE_MSG_PREFIX | 0x06;

        //请求转盘旋转
        int REQ_TURNTABLE = BASE_MSG_PREFIX | 0x07;
        //请求转盘旋转回复
        int RES_TURNTABLE = BASE_MSG_PREFIX | 0x08;

        //请求转盘历史记录
        int REQ_TURNTABLE_HISTORY = BASE_MSG_PREFIX | 0x09;
        //请求转盘历史记录回复
        int RES_TURNTABLE_HISTORY = BASE_MSG_PREFIX | 0x10;

        //请求玩家积分回返
        int REQ_POINT = BASE_MSG_PREFIX | 0x11;
        //同步玩家积分
        int SYNC_POINT = BASE_MSG_PREFIX | 0x12;

        //请求加载排行数据
        int REQ_LOAD_LEADERBOARD = BASE_MSG_PREFIX | 0x13;
        //请求加载排行数据回复
        int RES_LOAD_LEADERBOARD = BASE_MSG_PREFIX | 0x14;

        //请求排行记录
        int REQ_LOAD_LEADERBOARD_HISTORY = BASE_MSG_PREFIX | 0x15;
        //请求排行记录回复
        int RES_LOAD_LEADERBOARD_HISTORY = BASE_MSG_PREFIX | 0x16;

        //请求装盘充值情况
        int REQ_TURNTABLE_RECHARGE_INFO = BASE_MSG_PREFIX | 0x17;
        //请求装盘充值情况回复
        int RES_TURNTABLE_RECHARGE_INFO = BASE_MSG_PREFIX | 0x18;

        //请求积分大奖积分的阶梯奖励
        int REQ_POINTS_AWARD_LADDER_REWARD = BASE_MSG_PREFIX | 0x19;
        //请求积分大奖积分的阶梯奖励回复
        int RES_POINTS_AWARD_LADDER_REWARD = BASE_MSG_PREFIX | 0x20;

        //请求领取积分大奖积分的阶梯奖励
        int REQ_RECEIVE_POINTS_AWARD_LADDER_REWARD = BASE_MSG_PREFIX | 0x2a;
        //请求领取积分大奖积分的阶梯奖励回复
        int RES_RECEIVE_POINTS_AWARD_LADDER_REWARD = BASE_MSG_PREFIX | 0x2b;


    }

}
