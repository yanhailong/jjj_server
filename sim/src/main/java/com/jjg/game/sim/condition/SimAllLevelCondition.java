package com.jjg.game.sim.condition;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.base.condition.ConditionContext;
import com.jjg.game.core.base.condition.ConditionHandler;
import com.jjg.game.core.base.condition.MatchResultData;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.ConditionUpdate;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.base.condition.numeric.StateConditionEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.service.SimNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 模拟经营所有场景等级之和大于等于目标等级。
 */
@Component
public class SimAllLevelCondition implements ConditionHandler<PreparedCondition> {
    private static final Logger log = LoggerFactory.getLogger(SimAllLevelCondition.class);
    private final ConditionRuleRegistry conditionRules;
    private final SimPlayerContextRegistry contextRegistry;
    private final SimPlayerGameDao simPlayerGameDao;
    private final NodeConfig nodeConfig;
    private final SimNodeService simNodeService;
    private final Cache<Long, Integer> remoteAllLevelCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();
    @ClusterRpcReference
    private ToSimBridge toSimBridge;

    public SimAllLevelCondition(ConditionRuleRegistry conditionRules, SimPlayerContextRegistry contextRegistry,
                                SimPlayerGameDao simPlayerGameDao, NodeConfig nodeConfig,
                                SimNodeService simNodeService) {
        this.conditionRules = conditionRules;
        this.contextRegistry = contextRegistry;
        this.simPlayerGameDao = simPlayerGameDao;
        this.nodeConfig = nodeConfig;
        this.simNodeService = simNodeService;
    }

    @Override
    public String type() {
        return "simAllLevel";
    }

    @Override
    public EGameEventType eventType() {
        return EGameEventType.SIM_ALL_LEVEL;
    }

    @Override
    public PreparedCondition parse(List<String> args) {
        return conditionRules.prepare(ConditionSpec.from(1, args));
    }

    @Override
    public MatchResultData match(ConditionContext ctx, PreparedCondition config) {
        SimPlayerContext simContext = contextRegistry.getContext(ctx.player().getId());
        int allLevel;
        if (simContext != null) {
            allLevel = simContext.getSimBaseData() == null ? 0 : simContext.getSimBaseData().getAllLevel();
        } else if (NodeType.HALL.name().equalsIgnoreCase(nodeConfig.getType())) {
            allLevel = simPlayerGameDao.findAllLevelById(ctx.player().getId());
        } else {
            allLevel = remoteAllLevelCache.get(ctx.player().getId(), playerId -> getAllLevelRemotely(ctx));
        }
        ConditionUpdate update = config.evaluate(
                new StateConditionEvent(StateConditionEvent.Type.PLAYER_LEVEL, 0, allLevel, 0));
        return update.completed(update.apply(0))
                ? MatchResultData.match()
                : MatchResultData.notMatch(getErrorCode(), config.target(), allLevel);
    }

    private int getAllLevelRemotely(ConditionContext ctx) {
        long playerId = ctx.player().getId();
        ClusterClient client = simNodeService.getSimClusterClient(playerId, ctx.player().getIp());
        if (client == null) {
            log.warn("获取模拟经营总等级失败，未找到玩家 sim 节点 playerId={}", playerId);
            return 0;
        }
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            return toSimBridge.getSimAllLevel(playerId);
        } catch (Exception e) {
            log.error("跨节点获取模拟经营总等级异常 playerId={}", playerId, e);
            return 0;
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    @Override
    public MatchResultData addProgress(ConditionContext ctx, PreparedCondition config) {
        return MatchResultData.unknown();
    }

    @Override
    public int getErrorCode() {
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(1);
        return conditionCfg == null ? 0 : conditionCfg.getLanguageID();
    }
}
