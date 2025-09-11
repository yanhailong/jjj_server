package com.jjg.game.activity.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author lm
 * @date 2025/9/3 11:14
 */
public interface ActivityConstant {
    interface Common {
        //redis 锁时间
        int REDIS_LOCK = 500;
    }

    interface ActivityStatus {
        //未开始
        int NOT_START = 1;
        //运行中
        int RUNNING = 2;
        //已结束
        int ENDED = 3;
    }

    interface ClaimStatus {
        //不可领取
        int NOT_CLAIM = 1;
        //可领取
        int CAN_CLAIM = 2;
        //已领取
        int CLAIMED = 3;
    }

    //摇钱树
    interface CashCow {
        //默认分页大小
        int DEFAULT_SIZE = 30;
    }

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.ACTIVITY << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求活动详细信息
        int REQ_ACTIVITY_DETAIL_INFO = BASE_MSG_PREFIX | 0x01;
        //通过活动类型请求活动信息
        int REQ_ACTIVITY_INFO_BY_TYPE = BASE_MSG_PREFIX | 0x02;
        //请求领取活动奖励
        int REQ_ACTIVITY_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x03;
        //通知活动变化
        int NOTIFY_ACTIVITY_CHANGE = BASE_MSG_PREFIX | 0x04;
        //每日奖金
        //响应每日奖金活动类型信息
        int RES_PRIVILEGE_CARD_TYPE_INFO = BASE_MSG_PREFIX | 0x05;
        //响应每日奖励活动详细信息
        int RES_PRIVILEGE_CARD_DETAIL_INFO = BASE_MSG_PREFIX | 0x06;
        //响应每日奖金领取活动奖励
        int RES_PRIVILEGE_CARD_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x07;

        //摇钱树
        //响应摇钱树活动类型信息
        int RES_CASH_COW_TYPE_INFO = BASE_MSG_PREFIX | 0x08;
        //响应摇钱树活动详细信息
        int RES_CASH_COW_DETAIL_INFO = BASE_MSG_PREFIX | 0x09;
        //响应摇钱树领取活动奖励
        int RES_CASH_COW_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x0A;
        //摇钱树请求游戏记录信息
        int REQ_CASH_COW_RECORD = BASE_MSG_PREFIX | 0x0B;
        int RES_CASH_COW_RECORD = BASE_MSG_PREFIX | 0x0C;
        //摇钱树请求参加活动
        int REQ_CASH_COW_JOIN = BASE_MSG_PREFIX | 0x0D;
        //请求参加活动
        int REQ_ACTIVITY_PLAYER_JOIN = BASE_MSG_PREFIX | 0x0E;
        //响应摇钱树活动参加结果
        int RES_CASH_COW_JOIN = BASE_MSG_PREFIX | 0x0F;
        //摇钱树请求总池数量
        int REQ_CASH_COW_TOTAL_POOL = BASE_MSG_PREFIX | 0x10;
        int RES_CASH_COW_TOTAL_POOL = BASE_MSG_PREFIX | 0x11;


    }
}
