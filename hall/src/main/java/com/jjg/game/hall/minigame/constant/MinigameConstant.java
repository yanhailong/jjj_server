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
