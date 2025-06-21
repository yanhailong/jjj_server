package com.jjg.game.hall.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/6/10 17:04
 */
public interface HallMessageConst extends MessageConst {
    /**
     * 传入,返回参数类型
     */
    interface MsgBean {
        int TYPE = 0x6;

        //登录
        int REQ_LOGIN = 0x6001;
        int RES_LOGIN = 0x6002;

        //进入游戏
        int REQ_ENTER_GAME = 0x6003;
        int RES_ENTER_GAME = 0x6004;

        //选择场次
        int REQ_CHOOSE_WARE = 0X6005;
        int RES_CHOOSE_WARE = 0X6006;
    }
}
