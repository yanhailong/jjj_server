package com.jjg.game.poker.manager;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.service.SimNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongFunction;

/** Poker节点到SIM节点的RPC链路，目前用于赛季币查询与结算。 */
@Component
public class PokerRPCLinkManager {

    private static final Logger log = LoggerFactory.getLogger(PokerRPCLinkManager.class);

    @ClusterRpcReference
    private ToSimBridge toSimBridge;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private SimNodeService simNodeService;

    /**
     * 为已确认使用赛季币的房间创建账户并同步余额。
     * 是否使用赛季币由调用方根据房间交易道具判断；SIM不可达时仍返回零余额账户，避免错误回退为金币。
     */
    public PokerSeasonAccount bindSeasonAccount(long playerId, String ip) {
        PokerSeasonAccount account = new PokerSeasonAccount(playerId, ip);
        CommonResult<Long> result = getSeasonCoin(account);
        if (result.success() && result.data != null) {
            account.setBalance(result.data);
            log.info("Poker赛季账户绑定成功 playerId:{} balance:{}", playerId, result.data);
        } else {
            log.warn("Poker赛季账户绑定时查询余额失败 playerId:{} code:{}，本地余额暂按0处理",
                    playerId, result.code);
        }
        return account;
    }

    public CommonResult<Long> refreshSeasonCoin(PokerSeasonAccount account, String ip) {
        account.setIp(ip);
        CommonResult<Long> result = getSeasonCoin(account);
        updateBalance(account, result);
        return result;
    }

    public CommonResult<Long> deductSeasonCoin(PokerSeasonAccount account, long amount) {
        long txnId = nextTxnId();
        CommonResult<Long> result = seasonCoinCall(account,
                id -> toSimBridge.deductSeasonCoin(account.getPlayerId(), amount, id), txnId, "扣除");
        if (result.code == Code.EXCEPTION) {
            result = seasonCoinCall(account,
                    id -> toSimBridge.deductSeasonCoin(account.getPlayerId(), amount, id), txnId, "扣除重试");
        }
        updateBalance(account, result);
        return result;
    }

    public CommonResult<Long> addSeasonCoin(PokerSeasonAccount account, long amount) {
        long txnId = nextTxnId();
        CommonResult<Long> result = seasonCoinCall(account,
                id -> toSimBridge.addSeasonCoin(account.getPlayerId(), amount, id), txnId, "增加");
        if (result.code == Code.EXCEPTION) {
            result = seasonCoinCall(account,
                    id -> toSimBridge.addSeasonCoin(account.getPlayerId(), amount, id), txnId, "增加重试");
        }
        updateBalance(account, result);
        return result;
    }

    /** 斗仙牌大结算完成后按所属 SIM 节点批量通知，不阻塞牌桌收尾。 */
    public void notifyDouXianSettled(Map<Long, String> players) {
        for (SimPlayerBatch batch : groupBySimNode(players)) {
            notifyDouXianSettled(batch.client(), List.copyOf(batch.playerIds()));
        }
    }

    /** 斗仙牌每回合结算后，按所属 SIM 节点批量上报真人玩家的正向净赢。 */
    public void notifyDouXianWins(Map<Long, String> players, int transactionItemId,
                                  Map<Long, Long> playerWins) {
        if (playerWins == null || playerWins.isEmpty()) {
            return;
        }
        for (SimPlayerBatch batch : groupBySimNode(players)) {
            Map<Long, Long> batchWins = new HashMap<>();
            for (long playerId : batch.playerIds()) {
                Long win = playerWins.get(playerId);
                if (win != null && win > 0) {
                    batchWins.put(playerId, win);
                }
            }
            if (!batchWins.isEmpty()) {
                notifyDouXianWins(batch.client(), transactionItemId, Map.copyOf(batchWins));
            }
        }
    }

    private List<SimPlayerBatch> groupBySimNode(Map<Long, String> players) {
        if (players == null || players.isEmpty()) {
            return List.of();
        }
        Map<String, SimPlayerBatch> batches = new HashMap<>();
        for (Map.Entry<Long, String> entry : players.entrySet()) {
            long playerId = entry.getKey();
            ClusterClient client = simNodeService.getSimClusterClient(
                    playerId, entry.getValue() == null ? "" : entry.getValue());
            if (client == null) {
                log.warn("斗仙牌任务推进失败，未找到 SIM 节点 playerId:{}", playerId);
                continue;
            }
            batches.computeIfAbsent(client.marsNode.getNodePath(),
                    key -> new SimPlayerBatch(client, new ArrayList<>())).playerIds().add(playerId);
        }
        return List.copyOf(batches.values());
    }

    private void notifyDouXianSettled(ClusterClient client, List<Long> playerIds) {
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            rpcContext.asyncCall(() -> toSimBridge.onDouXianSettled(playerIds))
                    .whenComplete((result, throwable) -> {
                        if (throwable != null || result == null || !result.success()
                                || !Boolean.TRUE.equals(result.data)) {
                            log.warn("斗仙牌结算任务批量推进失败 playerIds:{}", playerIds, throwable);
                        }
                    });
        } catch (Exception e) {
            log.error("斗仙牌结算任务批量推进异常 playerIds:{}", playerIds, e);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    private void notifyDouXianWins(ClusterClient client, int transactionItemId,
                                   Map<Long, Long> playerWins) {
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            rpcContext.asyncCall(() -> toSimBridge.onDouXianWins(transactionItemId, playerWins))
                    .whenComplete((result, throwable) -> {
                        if (throwable != null || result == null || !result.success()
                                || !Boolean.TRUE.equals(result.data)) {
                            log.warn("斗仙牌赢钱任务批量推进失败 playerWins:{}", playerWins, throwable);
                        }
                    });
        } catch (Exception e) {
            log.error("斗仙牌赢钱任务批量推进异常 playerWins:{}", playerWins, e);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    private record SimPlayerBatch(ClusterClient client, List<Long> playerIds) {
    }

    private CommonResult<Long> getSeasonCoin(PokerSeasonAccount account) {
        ClusterClient client = resolveSimClient(account);
        if (client == null) {
            return new CommonResult<>(Code.EXCEPTION);
        }
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            CommonResult<Long> result = toSimBridge.getSeasonCoin(account.getPlayerId());
            return invalidSuccessResult(result) ? new CommonResult<>(Code.EXCEPTION) : result;
        } catch (Exception e) {
            log.error("Poker查询赛季币RPC异常 playerId:{}", account.getPlayerId(), e);
            return new CommonResult<>(Code.EXCEPTION);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    private CommonResult<Long> seasonCoinCall(PokerSeasonAccount account,
                                               LongFunction<CommonResult<Long>> rpc,
                                               long txnId, String action) {
        ClusterClient client = resolveSimClient(account);
        if (client == null) {
            return new CommonResult<>(Code.EXCEPTION);
        }
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            CommonResult<Long> result = rpc.apply(txnId);
            return invalidSuccessResult(result) ? new CommonResult<>(Code.EXCEPTION) : result;
        } catch (Exception e) {
            log.error("Poker赛季币{}RPC异常 playerId:{} txnId:{}", action, account.getPlayerId(), txnId, e);
            return new CommonResult<>(Code.EXCEPTION);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    private ClusterClient resolveSimClient(PokerSeasonAccount account) {
        ClusterClient client = account.getSimClient();
        if (client == null || clusterSystem.getClusterByPath(client.marsNode.getNodePath()) == null) {
            client = simNodeService.getSimClusterClient(account.getPlayerId(), account.getIp());
        }
        if (client == null) {
            account.setSimClient(null);
            return null;
        }

        // 同步RPC不能直接使用刚创建但连接池尚未就绪的ClusterClient。
        // getConnect()在连接池为空时只会异步发起连接并返回null；这里直接同步等待可用连接，
        // 避免RpcClientService在clusterClient.getConnect().writeWithFuture处空指针。
        try {
            var connect = client.getConnectSync();
            if (connect == null || !connect.isActive()) {
                if (connect != null) {
                    client.close(connect);
                }
                connect = client.getConnectSync();
            }
            if (connect == null || !connect.isActive()) {
                account.setSimClient(null);
                log.error("Poker获取SIM有效连接失败 playerId:{} nodePath:{}",
                        account.getPlayerId(), client.marsNode.getNodePath());
                return null;
            }
            account.setSimClient(client);
            return client;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            account.setSimClient(null);
            log.error("Poker等待SIM连接被中断 playerId:{} nodePath:{}",
                    account.getPlayerId(), client.marsNode.getNodePath(), e);
            return null;
        } catch (Exception e) {
            account.setSimClient(null);
            log.error("Poker建立SIM连接异常 playerId:{} nodePath:{}",
                    account.getPlayerId(), client.marsNode.getNodePath(), e);
            return null;
        }
    }
    private void updateBalance(PokerSeasonAccount account, CommonResult<Long> result) {
        if (result.success() && result.data != null) {
            account.setBalance(result.data);
        }
    }

    private boolean invalidSuccessResult(CommonResult<Long> result) {
        return result == null || (result.success() && result.data == null);
    }

    private long nextTxnId() {
        return ThreadLocalRandom.current().nextLong() | 1L;
    }
}
