package com.jjg.game.slots.game.steamAge;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author lihaocao
 * @date 2025/12/2 17:36
 */
public interface SteamAgeConstant {

    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.STEAM_AGE << MessageConst.MessageCommon.RIGHT_MOVE;
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
        int MINI_POOL_ID = 102500101;
        int MINOR_POOL_ID = 102500102;
        int MAJOR_POOL_ID = 102500103;
        int GRAND_POOL_ID = 102500104;
    }

    interface Status{
        int NORMAL = 0;
        int FREE = 1;
    }

    interface BaseElement{
        int ID_WILD = 10;
        int ID_SCATTER = 11;
        int ID_MINI = 12;
        int ID_MINOR = 13;
        int ID_MAJOR = 14;
        int ID_GRAND = 15;
    }

    interface SpecialMode{
        int NORMAL = 1;
        int FREE = 2;
        int JACKPOOL = 3;
    }

    interface SpecialPlay{
        //增加免费次数
        int TYPE_ADD_FREE_COUNT = 5;
        //新增一列进行摇奖 正常
        int TYPE_EXTEND_ICON_NORMAL= 8;
        //新增一列进行摇奖 免费转
        int TYPE_EXTEND_ICON_FREE = 9;
    }
}
