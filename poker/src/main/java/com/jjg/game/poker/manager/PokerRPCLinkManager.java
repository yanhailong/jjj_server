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
        ClusterClient cached = account.getSimClient();
        if (cached != null && clusterSystem.getClusterByPath(cached.marsNode.getNodePath()) != null) {
            return cached;
        }
        ClusterClient client = simNodeService.getSimClusterClient(account.getPlayerId(), account.getIp());
        if (client != null) {
            account.setSimClient(client);
        }
        return client;
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
