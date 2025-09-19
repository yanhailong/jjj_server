package com.jjg.game.hall.constant;

import com.jjg.game.common.constant.MessageConst;

/**
 * @author 11
 * @date 2025/9/18 14:22
 */
public interface ShopConstant {
    /**
     * 传入,返回参数类型
     */
    interface MsgBean {
        int BASE_MSG_PREFIX = MessageConst.MessageTypeDef.SHOP_TYPE << MessageConst.MessageCommon.RIGHT_MOVE;

        //请求商城
        int REQ_SHOP = BASE_MSG_PREFIX | 0X1;
        int RES_SHOP = BASE_MSG_PREFIX | 0X2;

        //下单
        int REQ_BUY_PRODUCT = BASE_MSG_PREFIX | 0X3;
        int RES_BUY_PRODUCT = BASE_MSG_PREFIX | 0X4;

        //充值返回
        int NOTIFY_PAY_CALLBACK = BASE_MSG_PREFIX | 0X99;
    }
}
