package com.jjg.game.core.rpc;

import java.util.List;

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

    /**
     * 后台完成玩家模拟经营新手引导。
     *
     * @param playerId 玩家ID
     * @param operationType 1=完成全部引导，2=完成指定引导
     * @param guideIds 指定完成的引导ID列表；完成全部时为空
     * @return 处理结果码
     */
    int finishSimGuide(long playerId, int operationType, List<Integer> guideIds);
}
