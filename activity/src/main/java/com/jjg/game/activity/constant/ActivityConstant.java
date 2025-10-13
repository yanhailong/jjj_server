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
        //限时活动类型
        int LIMIT_TYPE = 2;
        //开服活动类型
        int OPEN_SERVER_TYPE = 1;
        //循环活动类型
        int CYCLE_SERVER_TYPE = 3;
    }

    interface DailyLogin {
        //连续登录详情类型
        int CONTINUE_TYPE = 1;
        //累计获得详情类型
        int CUMULATIVE_TYPE = 2;
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
        //已购买
        int ALREADY_BUG = 4;
    }

    //摇钱树
    interface CashCow {
        //默认分页大小
        int DEFAULT_SIZE = 30;
        //摇钱树活动增长金额
        int CASH_COW_ROBOT_ADD_VALUE = 21;
        //摇钱树活动增加频率
        int CASH_COW_ROBOT_ADD_FREQUENCY = 22;
        //摇钱树活动当前奖金累计进入下一期的万分比比例
        int CASH_COW_ADD_NEXT_ROUND_PROPORTION = 23;
        //摇钱树活动当玩家产生有效打码量的金额万分比进入奖池
        int CASH_COW_ADD_POOL_PROPORTION = 24;
        //摇钱树活动每日免费获得的道具（重置时间跟随系统）
        int CASH_COW_FREE_ITEM = 25;
        //累计奖励类型领奖类型
        int CUMULATIVE_REWARDS_REWARD_TYPE = 4;
    }

    interface ScratchCards {
        //刮刮乐消耗道具
        int SCRATCH_CARDS_COST_ITEM = 28;
        //刮刮乐中奖类型
        int REWARDS_TYPE = 1;
        //刮刮乐礼包类型
        int GIFT_TYPE = 2;
    }

    //储钱罐
    interface PiggyBank {
        //每次下注金币的万分比飞入储钱罐
        int INCOME_PER_TEN_THOUSAND = 26;
        //自动领取邮件id
        int MAIL_ID = 21;
    }

    //推广分享
    interface SharePromote {
        //最大请求记录数
        int MAX_SIZE = 10;
        //排行榜奖励类型
        int RANK_REWARDS = 2;
        //最大记录数
        int MAX_RECODE_NUM = 20;
        //邮件id
        int MAIL_ID = 11;
    }

    interface OfficialAwards {
        //32 官方派奖：有效下注转换积分比例 ：有效下注 = X积分
        int EFFECTIVE_WATER_FLOW_CONVERT_RATIO = 32;
        //33 官方派奖：充值转换积分比例 ：充值金额 = X积分
        int RECHARGE_CONVERT_RATIO = 33;
        //35 机器人中奖设置
        int ROBOT_CFG = 35;
        //最大记录数
        int MAX_RECORD_NUM = 50;
        //每次获取最大记录数
        int GET_MAX_RECORD_NUM = 10;
    }

    //成长基金
    interface GrowthFund {
        int FREE = 1;
        int Charge = 2;
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
        //响应活动购买礼包结果
        int RES_ACTIVITY_BUY_GIFT = BASE_MSG_PREFIX | 0x17;
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
        //请求参加活动
        int REQ_ACTIVITY_PLAYER_JOIN = BASE_MSG_PREFIX | 0x0E;
        //响应摇钱树活动参加结果
        int RES_CASH_COW_JOIN = BASE_MSG_PREFIX | 0x0F;
        //摇钱树请求总池数量
        int REQ_CASH_COW_TOTAL_POOL = BASE_MSG_PREFIX | 0x10;
        int RES_CASH_COW_TOTAL_POOL = BASE_MSG_PREFIX | 0x11;
        //摇钱树请求领取免费道具
        int REQ_CASH_COW_FREE_REWARDS = BASE_MSG_PREFIX | 0x15;
        //响应摇钱树请求领取免费道具
        int RES_CASH_COW_FREE_REWARDS = BASE_MSG_PREFIX | 0x16;

        //储钱罐
        //响应储钱罐活动类型信息
        int RES_PIGGY_BANK_ACTIVITY_INFOS = BASE_MSG_PREFIX | 0x12;
        //响应每日奖励活动详细信息
        int RES_PIGGY_BANK_DETAIL_INFO = BASE_MSG_PREFIX | 0x13;
        //响应每日奖金领取活动奖励
        int RES_PIGGY_BANK_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x14;

        //刮刮乐
        //响应刮刮乐活动详细信息
        int RES_SCRATCH_CARDS_DETAIL_INFO = BASE_MSG_PREFIX | 0x19;
        //响应刮刮乐参加活动
        int RES_SCRATCH_CARDS_JOIN_ACTIVITY = BASE_MSG_PREFIX | 0x1A;
        //响应刮刮乐活动类型信息
        int RES_SCRATCH_CARDS_TYPE_INFO = BASE_MSG_PREFIX | 0x1B;

        //推广分享
        //响应推广分享活动详细信息
        int RES_SHARE_PROMOTE_DETAIL_INFO = BASE_MSG_PREFIX | 0x1C;
        //响应推广分享领取奖励
        int RES_SHARE_PROMOTE_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x1D;
        //响应推广分享活动类型信息
        int RES_SHARE_PROMOTE_TYPE_INFO = BASE_MSG_PREFIX | 0x1E;
        //请求绑定玩家
        int REQ_SHARE_PROMOTE_BIND_PLAYER = BASE_MSG_PREFIX | 0x1F;
        //请求领取收益奖励
        int REQ_SHARE_PROMOTE_CLAIM_PROFIT_REWARD = BASE_MSG_PREFIX | 0x20;
        //请求总览信息
        int REQ_SHARE_PROMOTE_GLOBAL_INFO = BASE_MSG_PREFIX | 0x21;
        //请求周排行榜
        int REQ_SHARE_PROMOTE_WEEK_RANK_INFO = BASE_MSG_PREFIX | 0x22;
        //请求推广分享我的收益排行榜信息
        int REQ_SHARE_PROMOTE_SELF_RANK_INFO = BASE_MSG_PREFIX | 0x24;
        //响应绑定玩家结果
        int RES_SHARE_PROMOTE_BIND_PLAYER = BASE_MSG_PREFIX | 0x25;
        //响应领取收益奖励结果
        int RES_SHARE_PROMOTE_CLAIM_PROFIT_REWARD = BASE_MSG_PREFIX | 0x26;
        //响应总览信息
        int RES_SHARE_PROMOTE_GLOBAL_INFO = BASE_MSG_PREFIX | 0x27;
        //响应周榜信息
        int RES_SHARE_PROMOTE_WEEK_RANK_INFO = BASE_MSG_PREFIX | 0x28;
        //响应推广分享我的收益排行榜信息
        int RES_SHARE_PROMOTE_SELF_RANK_INFO = BASE_MSG_PREFIX | 0x29;

        //等级礼包
        //请求等级礼包详情
        int REQ_PLAYER_LEVEL_PACK_DETAIL_INFO = BASE_MSG_PREFIX | 0x3A;
        //通知详细信息
        int NOTIFY_PLAYER_LEVEL_PACK_DETAIL_INFO = BASE_MSG_PREFIX | 0x3B;
        //请求领取玩家等级礼包
        int REQ_PLAYER_LEVEL_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x3C;
        //玩家领取等级礼包返回
        int RES_PLAYER_LEVEL_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x3D;


        //每日签到
        //每日签到活动信息
        int RES_DAILY_LOGIN_TYPE_INFO = BASE_MSG_PREFIX | 0x3E;
        //详细信息
        int RES_DAILY_LOGIN_DETAIL_INFO = BASE_MSG_PREFIX | 0x3F;
        //玩家领取每日签到返回
        int RES_DAILY_LOGIN_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x40;


        //首充
        //响应首充活动类型信息
        int RES_FIRST_PAYMENT_TYPE_INFO = BASE_MSG_PREFIX | 0x41;
        //响应首充活动详细信息
        int RES_FIRST_PAYMENT_DETAIL_INFO = BASE_MSG_PREFIX | 0x42;
        //响应首充领取活动奖励
        int RES_FIRST_PAYMENT_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x43;


        //官方派奖
        //响应官方派奖活动详细信息
        int RES_OFFICIAL_AWARDS_DETAIL_INFO = BASE_MSG_PREFIX | 0x44;
        //响应官方派奖参加活动
        int RES_OFFICIAL_AWARDS_JOIN_ACTIVITY = BASE_MSG_PREFIX | 0x45;
        //响应官方派奖活动类型信息
        int RES_OFFICIAL_AWARDS_TYPE_INFO = BASE_MSG_PREFIX | 0x46;
        //请求记录信息
        int REQ_OFFICIAL_AWARDS_RECORD = BASE_MSG_PREFIX | 0x47;
        int RES_OFFICIAL_AWARDS_RECORD = BASE_MSG_PREFIX | 0x48;
        //请求总奖池值
        int REQ_OFFICIAL_AWARDS_TOTAL_POOL = BASE_MSG_PREFIX | 0x49;
        int RES_OFFICIAL_AWARDS_TOTAL_POOL = BASE_MSG_PREFIX | 0x4A;

        //成长基金
        //成长基金活动信息
        int RES_GROWTH_FUND_CLAIM_REWARDS = BASE_MSG_PREFIX | 0x4B;
        //详细信息
        int RES_GROWTH_FUND_DETAIL_INFO = BASE_MSG_PREFIX | 0x4C;
        //玩家领取返回
        int RES_GROWTH_FUND_TYPE_INFO = BASE_MSG_PREFIX | 0x4D;


    }
}
