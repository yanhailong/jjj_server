package com.jjg.game.core.rpc;

import com.jjg.game.common.rpc.IGameRpc;

/**
 * gm调用大厅接口
 *
 * @author 11
 * @date 2026/1/19
 */
public interface GmToHallBridge extends IGameRpc {
    /**
     * 绑定或者解绑手机
     *
     * @param playerId
     * @param phone
     * @param type
     * @return
     */
    int playerBindPhone(long playerId, String phone, int type,boolean reward);

    /**
     * 后台验证短信成功后调用
     *
     * @param playerId
     * @param phone
     * @param type
     * @return
     */
    int afterVerifySmsSuccess(long playerId, String phone, int type);
}
