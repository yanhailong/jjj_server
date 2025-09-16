package com.jjg.game.slots.game.superstar;

import com.jjg.game.common.constant.MessageConst;

/**
 * 超级明星常量
 */
public interface SuperStarConstant {

    /**
     * 公共常量
     */
    interface Common {
        int MINI_POOL_ID = 100300101;
        int MINOR_POOL_ID = 100300102;
        int MAJOR_POOL_ID = 100300103;
        int GRAND_POOL_ID = 100300104;
        /**
         * 普通旋转
         */
        int SPECIAL_MODE_TYPE_NORMAL = 1;
        /**
         * 空元素的id
         */
        int EMPTY_ICON = 99;
    }

    /**
     * 协议
     */
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.SUPER_STAR_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x2;

        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x3;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x4;

        //请求奖池
        int REQ_POOL_VALUE = BASE_MSG_PREFIX | 0x5;
        int RES_POOL_VALUE = BASE_MSG_PREFIX | 0x6;

    }

}
