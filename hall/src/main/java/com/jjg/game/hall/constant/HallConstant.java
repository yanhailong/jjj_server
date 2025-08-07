package com.jjg.game.hall.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/6/10 17:04
 */
public interface HallConstant  {

    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH;
    }

    interface VerCode{
        //验证码类型
        int TYPE_BIND_PHONE = 0;
        int TYPE_BIND_EMAIL = 1;

        //验证码范围
        int CODE_MIN = 1000;
        int CODE_MAX = 9999;
    }

    /**
     * 传入,返回参数类型
     */
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.HALL_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

        //登录
        int REQ_LOGIN = BASE_MSG_PREFIX | 0x1;
        int RES_LOGIN = BASE_MSG_PREFIX | 0x2;

        //进入游戏
        int REQ_ENTER_GAME = BASE_MSG_PREFIX | 0x3;
        int RES_ENTER_GAME = BASE_MSG_PREFIX | 0x4;

        //选择场次
        int REQ_CHOOSE_WARE = BASE_MSG_PREFIX | 0x5;
        int RES_CHOOSE_WARE = BASE_MSG_PREFIX | 0x6;

        //获取奖池信息
        int REQ_POOL = BASE_MSG_PREFIX | 0x7;
        int RES_POOL = BASE_MSG_PREFIX | 0x8;

        //获取玩家信息
        int REQ_QUERY_PLAYER_INFO = BASE_MSG_PREFIX | 0x9;
        int RES_QUERY_PLAYER_INFO = BASE_MSG_PREFIX | 0xA;

        //修改玩家信息
        int REQ_CHANGE_PLAYER_INFO = BASE_MSG_PREFIX | 0xB;
        int RES_CHANGE_PLAYER_INFO = BASE_MSG_PREFIX | 0xC;

        //获取验证码
        int REQ_VER_CODE = BASE_MSG_PREFIX | 0xD;
        int RES_VER_CODE = BASE_MSG_PREFIX | 0xE;

        //确认验证码
        int REQ_CONFIRM_VER_CODE = BASE_MSG_PREFIX | 0xF;
        int RES_CONFIRM_VER_CODE = BASE_MSG_PREFIX | 0x10;
    }
}
