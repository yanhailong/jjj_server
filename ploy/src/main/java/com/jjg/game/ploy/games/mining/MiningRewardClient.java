package com.jjg.game.ploy.games.mining;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.rpc.MiningRewardBridge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MiningRewardClient {
    @Autowired private ClusterSystem cluster;
    @Autowired private RedisTemplate redis;
    @ClusterRpcReference private MiningRewardBridge bridge;

    public CommonResult<ItemOperationResult> grant(long playerId, Map<Integer, Long> rewards,
                                                  AddType source, String deliveryId) {
        String ownerPath = (String) redis.opsForHash().get("simnode", playerId);
        ClusterClient owner = ownerPath == null ? null : cluster.getClusterByPath(ownerPath);
        // 未发出 RPC 的失败明确没有发奖，可以由原挖矿流程退还消耗和限购次数。
        if (owner == null) return new CommonResult<>(Code.NOT_FOUND);
        GameRpcContext rpc = GameRpcContext.getContext();
        RpcReqParameterBuilder previous = rpc.getReqParameterBuilder();
        try {
            rpc.setReqParameterBuilder(RpcReqParameterBuilder.create().addClusterClient(owner).setRetryTimesPerClient(0));
            CommonResult<ItemOperationResult> result = bridge.grantMiningRoleReward(playerId, rewards, source, deliveryId);
            // 超时、空响应、发奖阶段异常不能按失败退款；保留 MiningState.delivery 等待核账。
            if (result == null || result.code == Code.EXCEPTION || result.code == Code.FAIL)
                throw new MiningException(Code.FAIL, "ROLE_DELIVERY_REQUIRES_RECONCILIATION");
            return result;
        } finally { rpc.setReqParameterBuilder(previous); }
    }
}
