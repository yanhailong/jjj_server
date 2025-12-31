package com.jjg.game.core.rpc;

import com.jjg.game.common.rpc.IGameRpc;

public interface BackendBridge extends IGameRpc {
    /**
     * 充值
     *
     * @param selfOrderId  本系统生成的订单id
     * @param channelOrderId 第三方订单id
     * @param price        价格
     * @param rechargeType 充值类型
     * @return
     */
    int recharge(String selfOrderId, String channelOrderId, long price, int rechargeType);
}
