package com.jjg.game.hall.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/6/10 17:04
 */
public interface HallConstant {

    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH;


    }

    interface VerCode {
        //验证码类型
        int TYPE_BIND_PHONE = 0;
        int TYPE_BIND_EMAIL = 1;

        //验证码范围
        int CODE_MIN = 1000;
        int CODE_MAX = 9999;
    }

    interface Avatar {
        //头像
        int TYPE_AVATAR = 0;
        //头像框
        int TYPE_FRAME = 1;
        //国旗
        int TYPE_NATIONAL = 2;
        //称号
        int TYPE_TITLE = 3;
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

        //选择头像等信息(头像，头像框，称号，国旗)
        int REQ_SELECT_AVATAR = BASE_MSG_PREFIX | 0x11;
        int RES_SELECT_AVATAR = BASE_MSG_PREFIX | 0x12;

        //获取背包
        int REQ_GET_PACK = BASE_MSG_PREFIX | 0x13;
        int RES_GET_PACK = BASE_MSG_PREFIX | 0x14;

        //使用道具
        int REQ_USE_ITEM = BASE_MSG_PREFIX | 0x15;
        int RES_USE_ITEM = BASE_MSG_PREFIX | 0x16;

        //获取所有头像信息
        int REQ_ALL_AVATAR = BASE_MSG_PREFIX | 0x17;
        int RES_ALL_AVATAR = BASE_MSG_PREFIX | 0x18;

        //获取邮件
        int REQ_GET_MAILS = BASE_MSG_PREFIX | 0x19;
        int RES_GET_MAILS = BASE_MSG_PREFIX | 0x1A;

        //阅读邮件
        int REQ_READ_MAIL = BASE_MSG_PREFIX | 0x1B;
        int RES_READ_MAIL = BASE_MSG_PREFIX | 0x1C;

        //领取邮件内的道具
        int REQ_GET_MAIL_ITEMS = BASE_MSG_PREFIX | 0x1D;
        int RES_GET_MAIL_ITEMS = BASE_MSG_PREFIX | 0x1E;

        //删除一封邮件
        int REQ_REMOVE_MAIL = BASE_MSG_PREFIX | 0x1F;
        int RES_REMOVE_MAIL = BASE_MSG_PREFIX | 0x20;

        //删除已读邮件
        int REQ_REMOVE_READ_MAILS = BASE_MSG_PREFIX | 0x21;
        int RES_REMOVE_READ_MAILS = BASE_MSG_PREFIX | 0x22;

        //一键领取邮件内的道具
        int REQ_GET_ALL_MAILS_ITEMS = BASE_MSG_PREFIX | 0x23;
        int RES_GET_ALL_MAILS_ITEMS = BASE_MSG_PREFIX | 0x24;

        // 请求功能开放列表
        int REQ_FUNCTION_OPEN_LIST = BASE_MSG_PREFIX | 0x25;
        int RES_FUNCTION_OPEN_LIST = BASE_MSG_PREFIX | 0X26;
        int NOTIFY_FUNCTION_OPEN_LIST = BASE_MSG_PREFIX | 0X27;

        //保险箱转移金币
        int REQ_TRANS_SAFE_BOX_GOLD = BASE_MSG_PREFIX | 0x28;
        int RES_TRANS_SAFE_BOX_GOLD = BASE_MSG_PREFIX | 0x29;

        //保险箱转移钻石
        int REQ_TRANS_SAFE_BOX_DIAMOND = BASE_MSG_PREFIX | 0x2A;
        int RES_TRANS_SAFE_BOX_DIAMOND = BASE_MSG_PREFIX | 0x2B;
    }
}
