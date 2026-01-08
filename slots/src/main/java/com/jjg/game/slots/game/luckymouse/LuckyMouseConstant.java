package com.jjg.game.slots.game.luckymouse;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.slots.constant.SlotsConst;

public interface LuckyMouseConstant {
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.LUCKY_MOUSE << MessageConst.MessageCommon.RIGHT_MOVE;
        //请求配置
        int REQ_CONFIG_INFO = BASE_MSG_PREFIX | 0x1;
        int RES_CONFIG_INFO = BASE_MSG_PREFIX | 0x2;

        //开始游戏
        int REQ_START_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_START_GAME = BASE_MSG_PREFIX | 0x4;

        //请求奖池
        int REQ_POOL_INFO = BASE_MSG_PREFIX | 0x5;
        int RES_POOL_INFO = BASE_MSG_PREFIX | 0x6;
    }

    interface Status{
        int NORMAL = SlotsConst.Status.NORMAL;
        //免费模式
        int FREE = SlotsConst.Status.FREE;
        //触发真免费
        int REAL_FREE = 2;
        //触发假免费
        int FAKE_FREE = 3;
    }

    interface BaseElement{
        int ID_WILD = 11;
        int ID_SCATTER = 12;
        int ID_ADDFREEE = 13;
        int ID_MINI = 14;
        int ID_MINOR = 15;
        int ID_MAJOR = 16;
        int ID_GRAND = 17;
    }

    interface SpecialMode{
        int NORMAL = 1;
        int FREE = 2;
        int JACKPOOL = 3;
    }

    interface SpecialPlay{
        //免费游戏出现在2、3、4、5轴时变成百搭，并一直粘连，直至退出此模式
        int TYPE_STICKY_WILD= 1;
        //增加免费次数
        int TYPE_ADD_FREE_COUNT = 5;
    }
}
