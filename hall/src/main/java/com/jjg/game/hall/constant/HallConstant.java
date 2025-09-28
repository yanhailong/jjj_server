package com.jjg.game.hall.constant;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.MessageConst;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/10 17:04
 */
public interface HallConstant {

    interface Common {
        //excel配置所在目录
        String SAMPLE_PATH = CoreConst.Common.SAMPLE_ROOT_PATH;


    }

    interface Casino {
        //升级完成全部刷新类型
        List<Integer> ALL_REFLUSH_TYPE = List.of(4, 5);
    }

    interface VerCode {
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

        //请求我的赌场信息
        int REQ_CASINO_INFO = BASE_MSG_PREFIX | 0X28;
        int RES_CASINO_INFO = BASE_MSG_PREFIX | 0X29;

        //请求一键领取赌场奖励
        int REQ_CASINO_CLAIM_ALL_REWARDS = BASE_MSG_PREFIX | 0X2A;
        //请求领取赌场奖励
        int REQ_CASINO_CLAIM_REWARDS = BASE_MSG_PREFIX | 0X2B;
        int RES_CASINO_CLAIM_REWARDS = BASE_MSG_PREFIX | 0X2C;

        //请求升级机台
        int REQ_CASINO_UPGRADE_MACHINE = BASE_MSG_PREFIX | 0X2D;
        int RES_CASINO_UPGRADE_MACHINE = BASE_MSG_PREFIX | 0X2E;
        //请求楼层操作
        int REQ_CASINO_FLOOR_OPERATION = BASE_MSG_PREFIX | 0X2F;
        int RES_CASINO_FLOOR_OPERATION = BASE_MSG_PREFIX | 0X30;
        //请求购买一键领取
        int REQ_CASINO_BUY_CLAIM_ALL_REWARDS = BASE_MSG_PREFIX | 0X31;
        int RES_CASINO_BUY_CLAIM_ALL_REWARDS = BASE_MSG_PREFIX | 0X32;
        //请求雇佣职员
        int REQ_CASINO_EMPLOY_STAFF = BASE_MSG_PREFIX | 0X33;
        int RES_CASINO_EMPLOY_STAFF = BASE_MSG_PREFIX | 0X34;

        //保险箱转移金币
        int REQ_TRANS_SAFE_BOX_GOLD = BASE_MSG_PREFIX | 0x35;
        int RES_TRANS_SAFE_BOX_GOLD = BASE_MSG_PREFIX | 0x36;

        //保险箱转移钻石
        int REQ_TRANS_SAFE_BOX_DIAMOND = BASE_MSG_PREFIX | 0x37;
        int RES_TRANS_SAFE_BOX_DIAMOND = BASE_MSG_PREFIX | 0x38;

        //收藏游戏
        int REQ_LIKE_GAME = BASE_MSG_PREFIX | 0x39;
        int RES_LIKE_GAME = BASE_MSG_PREFIX | 0x3A;

        //取消收藏游戏
        int REQ_CANCEL_LIKE_GAME = BASE_MSG_PREFIX | 0x3B;
        int RES_CANCEL_LIKE_GAME = BASE_MSG_PREFIX | 0x3C;

        //通知游戏列表
        int NOTIFY_GAME_LIST = BASE_MSG_PREFIX | 0x3D;
        //通知我的赌场简单信息变化
        int NOTIFY_CASINO_SIMPLE_CHANGE = BASE_MSG_PREFIX | 0X3F;

        //请求退出我的赌场
        int REQ_CASINO_EXIT = BASE_MSG_PREFIX | 0X3E;
        int RES_CASINO_EXIT = BASE_MSG_PREFIX | 0X40;

        //请求vip信息
        int REQ_VIP_INFO = BASE_MSG_PREFIX | 0X41;
        int RES_VIP_INFO = BASE_MSG_PREFIX | 0X42;

        //请求vip请求领取礼包
        int REQ_VIP_CLAIM_GIFT_REWARD = BASE_MSG_PREFIX | 0X43;
        int RES_VIP_CLAIM_GIFT_REWARD = BASE_MSG_PREFIX | 0X44;

    }
}
