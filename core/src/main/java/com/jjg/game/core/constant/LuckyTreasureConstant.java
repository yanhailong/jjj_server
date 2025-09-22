package com.jjg.game.core.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * 夺宝奇兵常量
 */
public interface LuckyTreasureConstant {

    /**
     * 公共配置
     */
    interface Common {
        /**
         * 夺宝奇兵最大条数 全局表配置id
         */
        int LUCKY_TREASURE_GLOBAL_CONFIG_ID = 29;
        /**
         * id
         */
        int GAME_ID = 1001;
    }

    /**
     * 缓存key
     */
    interface RedisKey {
        /**
         * 夺宝奇兵配置缓存key
         */
        String LUCKY_TREASURE_CONFIG = "luckyTreasure:config:";

        /**
         * 活跃活动状态key前缀
         */
        String LUCKY_TREASURE_ACTIVE = "luckyTreasure:active:";

        /**
         * 每日期号计数器key前缀
         */
        String LUCKY_TREASURE_DAILY_COUNTER = "luckyTreasure:dailyCounter:";

        /**
         * 活动数据缓存key前缀
         */
        String LUCKY_TREASURE_ROUND_DATA = "luckyTreasure:roundData:";

        /**
         * 期号映射key前缀
         */
        String LUCKY_TREASURE_ROUND_DATA_ISSUE = "luckyTreasure:issue:";

        /**
         * 用来同步每个节点变化的夺宝奇兵活动库存
         */
        String LUCKY_TREASURE_UPDATE_CHANEL = "luckyTreasure:updateInfo:chanel:";

        /**
         * 库存发生变化的期号
         */
        String LUCKY_TREASURE_UPDATE_INFO = "luckyTreasure:updateInfo:";

    }

    /**
     * redis锁
     */
    interface RedisLock {
        /**
         * 夺宝奇兵初始化锁
         */
        String LUCKY_TREASURE_INIT = "luckyTreasure:init";

        /**
         * 启动新活动锁前缀
         */
        String LUCKY_TREASURE_START = "luckyTreasure:start:";

        /**
         * 结束活动锁前缀
         */
        String LUCKY_TREASURE_END = "luckyTreasure:end:";

        /**
         * 购买夺宝奇兵锁前缀
         */
        String LUCKY_TREASURE_BUY = "luckyTreasure:buy:";

        /**
         * 领取夺宝奇兵奖励锁前缀
         */
        String LUCKY_TREASURE_RECEIVE = "luckyTreasure:receive:";
    }

    /**
     * 协议
     */
    interface Message {
        int BASE_MSG_MASK = MessageConst.MessageTypeDef.MINIGAME << MessageConst.MessageCommon.RIGHT_MOVE;

        /**
         * 请求夺宝奇兵详情
         */
        int REQ_LUCKY_TREASURE = BASE_MSG_MASK | 0x10;
        int RES_LUCKY_TREASURE = BASE_MSG_MASK | 0x11;

        /**
         * 请求购买夺宝奇兵道具
         */
        int REQ_BUY_LUCKY_TREASURE = BASE_MSG_MASK | 0x12;
        int RES_BUY_LUCKY_TREASURE = BASE_MSG_MASK | 0x13;

        /**
         * 请求领取夺宝奇兵道具
         */
        int REQ_RECEIVE_LUCKY_TREASURE = BASE_MSG_MASK | 0x14;
        int RES_RECEIVE_LUCKY_TREASURE = BASE_MSG_MASK | 0x15;

        /**
         * 请求查看自己参与的所有夺宝奇兵
         */
        int REQ_LUCKY_TREASURE_RECORD = BASE_MSG_MASK | 0x16;
        int RES_LUCKY_TREASURE_RECORD = BASE_MSG_MASK | 0x17;

        /**
         * 请求查看所有的开奖记录
         */
        int REQ_LUCKY_TREASURE_AWARD_HISTORY = BASE_MSG_MASK | 0x18;
        int RES_LUCKY_TREASURE_AWARD_HISTORY = BASE_MSG_MASK | 0x19;

        /**
         * 通知更新夺宝奇兵库存信息
         */
        int NOTIFY_LUCKY_TREASURE_UPDATE = BASE_MSG_MASK | 0x1A;
    }
}
