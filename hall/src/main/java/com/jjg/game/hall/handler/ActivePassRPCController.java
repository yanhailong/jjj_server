package com.jjg.game.hall.handler;

import com.jjg.game.sim.data.ActivePassPurchase;
import com.jjg.game.sim.pb.res.ResActivePass;
import com.jjg.game.sim.service.ActivePassOrderService;
import com.jjg.game.sim.service.ActivePassService;
import com.jjg.game.common.rpc.RpcCallSetting;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.manager.SimManager;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.service.SimNodeService;
import com.jjg.game.common.cluster.ClusterSystem;
import org.springframework.stereotype.Component;

/** 跨节点接口只在玩家所属大厅的串行玩家线程执行。 */
@Component
public class ActivePassRPCController implements ToSimBridge.ActivePassBridge {
    private final SimManager sim;
    private final ActivePassService service;
    private final ActivePassOrderService orders;
    private final SimNodeService nodes;
    private final ClusterSystem cluster;
    public ActivePassRPCController(SimManager sim, ActivePassService service, ActivePassOrderService orders,
                                    SimNodeService nodes, ClusterSystem cluster) {
        this.sim = sim; this.service = service; this.orders = orders;
        this.nodes = nodes; this.cluster = cluster;
    }
    private SimPlayerContext context(long playerId) {
        String owner = nodes.get(playerId);
        // RPC框架会先尝试本地实现；不允许非归属大厅绕过路由创建另一份数据。
        if (owner != null && !owner.equals(cluster.getNodePath())) {
            throw new IllegalStateException("活跃通行证请求不在玩家归属节点: " + playerId);
        }
        return sim.createContextByPlayerId(playerId);
    }
    @Override @RpcCallSetting(processorModKey = "#arg0")
    public ResActivePass activePassInfo(long playerId) {
        return service.info(context(playerId));
    }
    @Override @RpcCallSetting(processorModKey = "#arg0")
    public ResActivePass claimActivePassTask(long playerId, int passId, int taskId, int day) {
        return service.claimTask(context(playerId), passId, taskId, day);
    }
    @Override @RpcCallSetting(processorModKey = "#arg0")
    public ResActivePass claimActivePassRewards(long playerId, int passId, int rewardId, int track) {
        return service.claimRewards(context(playerId), passId, rewardId, track);
    }
    @Override @RpcCallSetting(processorModKey = "#arg0")
    public ResActivePass buyActivePassPoints(long playerId, int passId, int count, int expectedPurchasedPoints) {
        return service.buyPoints(context(playerId), passId, count, expectedPurchasedPoints);
    }
    @Override @RpcCallSetting(processorModKey = "#arg0")
    public ActivePassPurchase prepareActivePassOrder(long playerId, int passId, int track) {
        return orders.prepare(context(playerId), passId, track);
    }
    @Override @RpcCallSetting(processorModKey = "#arg0")
    public boolean receiveActivePassOrder(long playerId, String orderId) {
        return orders.receive(context(playerId), orderId);
    }
}
