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

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.ACTIVITY << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求活动详细信息
        int REQ_ACTIVITY_DETAIL_INFO = BASE_MSG_PREFIX | 0x01;
        //通过活动类型请求活动信息
        int REQ_ACTIVITY_INFO_BY_TYPE = BASE_MSG_PREFIX | 0x02;
        //请求领取活动奖励
        int REQ_ACTIVITY_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x03;


        //每日奖励响应奖励领取
        int RES_PRIVILEGE_CARD_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x04;
    }
}
