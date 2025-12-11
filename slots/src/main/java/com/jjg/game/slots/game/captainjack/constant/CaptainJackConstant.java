package com.jjg.game.slots.game.captainjack.constant;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.game.thor.ThorConstant;

/**
 * @author 11
 * @date 2025/8/1 17:36
 */
public interface CaptainJackConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.CAPTAIN_JACK << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CAPTAIN_JACK_ENTER_GAME = BASE_MSG_PREFIX | 0x1;
        int RES_CAPTAIN_JACK_ENTER_GAME = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_CAPTAIN_JACK_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_CAPTAIN_JACK_START_GAME = BASE_MSG_PREFIX | 0x4;

    }

    interface BaseElement {
        //免费图标
        int FREE_ICON = 12;
        //宝箱图标
        int TREASURE_ICON = 10;
    }

    interface Status {
        int NORMAL = 0;
        //免费
        int FREE = 1;
        //宝箱
        int TREASURE_CHEST = 2;
    }

    interface SpecialMode {
        int NORMAL = 1;
        int FREE = 2;
        //免费游戏
        int MINI_GAME = 3;
        //大奖
        int JACK_POOL = 4;
    }

    interface SpecialPlay {
        //免费游戏倍率配置id
        int FREE_GAME_CONFIG_ID = 502100;

    }
}
