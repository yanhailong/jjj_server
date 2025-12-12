package com.jjg.game.slots.game.basketballSuperstar;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author lihaocao
 * @date 2025/12/2 17:36
 */
public interface BasketballSuperstarConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.BASKETBALL_SUPERSTAR << MessageConst.MessageCommon.RIGHT_MOVE;
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

    interface Common{
        int MINI_POOL_ID = 101700101;
        int MINOR_POOL_ID = 101700102;
        int MAJOR_POOL_ID = 101700103;
        int GRAND_POOL_ID = 101700104;
    }

    interface Status{
        int NORMAL = 0;
        int FREE = 1;
    }

    interface Element{
        int WILD = 11;
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
