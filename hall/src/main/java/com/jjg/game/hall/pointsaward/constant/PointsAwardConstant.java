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
         * 玩家积分大奖数据缓存key
         */
        String POINTS_AWARD_DATA = "pointsAwardData:";
        /**
         * 玩家积分原子缓存key
         */
        String POINTS_AWARD_DATA_POINTS = "pointsAwardDataPoints:";
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

    }

}
