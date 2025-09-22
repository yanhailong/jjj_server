package com.jjg.game.hall.minigame.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * 小游戏常量
 */
public interface MinigameConstant {

    /**
     * 游戏id
     */
    interface GameId {
        /**
         * 幸运夺宝
         */
        int LUCKY_TREASURE = 1001;
    }

    /**
     * 缓存key
     */
    interface RedisKey {
        /**
         * 小游戏配置
         */
        String MINIGAME_CONFIG = "minigame:config";
        /**
         * 游戏状态,决定了小游戏开启关闭
         */
        String MINIGAME_STATUS = "minigame:status";
        /**
         * 记录的首次开服时间
         */
        String MINIGAME_OPEN_SERVER_TIME_FIRST = "minigame:openServerTime:first:";
        /**
         * 记录的开服时间 需要根据该时间计算期数以及轮询次数
         */
        String MINIGAME_OPEN_SERVER_TIME = "minigame:openServerTime";

    }

    /**
     * redis锁
     */
    interface RedisLock {
        /**
         * 小游戏管理器初始化锁
         */
        String MINIGAME_INIT_LOCK = "minigameConfigInitLock";

    }

    /**
     * 协议
     */
    interface Message {
        int BASE_MSG_MASK = MessageConst.MessageTypeDef.MINIGAME << MessageConst.MessageCommon.RIGHT_MOVE;
        /**
         * 请求小游戏列表
         */
        int REQ_MINIGAME_LIST = BASE_MSG_MASK | 0x01;
        int RES_MINIGAME_LIST = BASE_MSG_MASK | 0x02;

    }

}
