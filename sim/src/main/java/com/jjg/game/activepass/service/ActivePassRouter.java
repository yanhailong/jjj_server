package com.jjg.game.activepass.service;

import com.jjg.game.activepass.bridge.ActivePassBridge;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.service.SimNodeService;
import org.springframework.stereotype.Component;
import java.util.function.Function;
import java.util.function.Supplier;

/** 所有接口和支付到账共用同一归属节点路由。 */
@Component
public class ActivePassRouter {
    private final SimPlayerContextRegistry contexts;
    private final SimNodeService nodes;
    @ClusterRpcReference
    private ActivePassBridge bridge;

    public ActivePassRouter(SimPlayerContextRegistry contexts, SimNodeService nodes) {
        this.contexts = contexts;
        this.nodes = nodes;
    }
    public <T> T execute(long playerId, String ip, Supplier<T> local, Function<ActivePassBridge, T> remote) {
        if (contexts.getContext(playerId) != null) { return local.get(); }
        var client = nodes.getSimClusterClient(playerId, ip);
        if (client == null) { throw new IllegalStateException("玩家SIM节点不可用: " + playerId); }
        GameRpcContext context = GameRpcContext.getContext();
        RpcReqParameterBuilder previous = context.getReqParameterBuilder();
        try {
            context.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            return remote.apply(bridge);
        } finally { context.setReqParameterBuilder(previous); }
    }
}
