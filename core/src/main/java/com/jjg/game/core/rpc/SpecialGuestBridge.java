package com.jjg.game.core.rpc;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.data.CommonResult;

/** 充值节点向 Hall 发放指定场景特殊游客。 */
public interface SpecialGuestBridge extends IGameRpc {

    /**
     * 将现金购买的特殊游客幂等发放到下单场景。
     */
    CommonResult<Boolean> receiveSpecialGuest(long playerId, int casinoId, int itemId, long count, String orderId);
}
