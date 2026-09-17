package com.jjg.game.ploy.games.mining;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.service.SimNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 兑换提交成功后，将本次事实投递到玩家所属 SIM 节点。 */
@Service
public class MiningConditionClient {
    private static final Logger log = LoggerFactory.getLogger(MiningConditionClient.class);
    private final SimNodeService simNodeService;
    @ClusterRpcReference
    private ToSimBridge toSimBridge;

    public MiningConditionClient(SimNodeService simNodeService) {
        this.simNodeService = simNodeService;
    }

    public void onExchange(long playerId, int itemId, long count) {
        GameRpcContext rpc = GameRpcContext.getContext();
        RpcReqParameterBuilder previous = rpc.getReqParameterBuilder();
        try {
            ClusterClient owner = simNodeService.getSimClusterClient(playerId, "");
            if (owner == null) {
                log.warn("挖矿兑换条件上报失败，SIM 节点不存在 playerId={}", playerId);
                return;
            }
            rpc.withReqParameterBuilder(RpcReqParameterBuilder.create().addClusterClient(owner)
                    .setRetryTimesPerClient(0));
            rpc.asyncCall(() -> toSimBridge.onMiningExchange(playerId, itemId, count))
                    .whenComplete((result, error) -> {
                        if (error != null || result == null || !result.success()) {
                            log.warn("挖矿兑换条件上报失败 playerId={},itemId={},count={}",
                                    playerId, itemId, count, error);
                        }
                    });
        } catch (Exception e) {
            log.error("挖矿兑换条件上报异常 playerId={},itemId={},count={}", playerId, itemId, count, e);
        } finally {
            rpc.setReqParameterBuilder(previous);
        }
    }
}
