package com.jjg.game.core.rpc;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;

import java.util.Map;

/** 仅供 PLOY 转交角色奖励；复用大厅联盟商店的通用发奖入口，不属于客户端 PB。 */
public interface MiningRewardBridge extends IGameRpc {
    CommonResult<ItemOperationResult> grantMiningRoleReward(long playerId, Map<Integer, Long> rewards,
                                                           AddType source, String deliveryId);
}
