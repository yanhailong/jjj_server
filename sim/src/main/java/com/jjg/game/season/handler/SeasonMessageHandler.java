package com.jjg.game.season.handler;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.season.constant.SeasonConstant;
import com.jjg.game.season.pb.req.*;
import com.jjg.game.season.pb.res.ResSeasonMatch;
import com.jjg.game.season.pb.res.ResSeasonTrialProgress;
import com.jjg.game.season.service.SeasonService;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.service.SimNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * @author 11
 * @date 2026/7/9
 */
@Component
@MessageType(MessageConst.MessageTypeDef.SEASON)
public class SeasonMessageHandler implements GmListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SeasonService seasonService;
    @Autowired
    private SimPlayerContextRegistry simPlayerContextRegistry;
    @Autowired
    private SimNodeService simNodeService;
    @ClusterRpcReference
    private ToSimBridge toSimBridge;

    @Command(SeasonConstant.MsgBean.REQ_SEASON_INFO)
    public void reqSeasonInfo(PlayerController playerController, ReqSeasonInfo req) {
        execute(playerController, ctx -> ctx.send(seasonService.info(ctx)));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_SHOP)
    public void reqSeasonShop(PlayerController playerController, ReqSeasonShop req) {
        execute(playerController, ctx -> ctx.send(seasonService.shop(ctx)));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_BUY)
    public void reqSeasonBuy(PlayerController playerController, ReqSeasonBuy req) {
        execute(playerController, ctx -> ctx.send(seasonService.buy(ctx, req.shopId, req.count)));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_GEMS)
    public void reqSeasonGems(PlayerController playerController, ReqSeasonGems req) {
        execute(playerController, ctx -> ctx.send(seasonService.gems(ctx)));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_EQUIP_GEM)
    public void reqSeasonEquipGem(PlayerController playerController, ReqSeasonEquipGem req) {
        execute(playerController, ctx -> ctx.send(seasonService.equip(ctx, req.slot, req.itemId)));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_CRAFT_GEM)
    public void reqSeasonCraftGem(PlayerController playerController, ReqSeasonCraftGem req) {
        execute(playerController, ctx -> ctx.send(seasonService.craft(ctx, req.itemIds, req.keepItemId)));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_MATCH)
    public void reqSeasonMatch(PlayerController playerController, ReqSeasonMatch req) {
        long playerId = playerController.playerId();
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx != null) {
            playerController.send(seasonService.match(ctx, req.gameType, req.stake));
            return;
        }
        playerController.send(remoteSeasonMatch(playerController, req));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_MATCH_HISTORY)
    public void reqSeasonMatchHistory(PlayerController playerController, ReqSeasonMatchHistory req) {
        execute(playerController, ctx -> ctx.send(seasonService.history(ctx)));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_RANK)
    public void reqSeasonRank(PlayerController playerController, ReqSeasonRank req) {
        execute(playerController, ctx -> ctx.send(seasonService.rank(ctx, req.limit)));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_TRIALS)
    public void reqSeasonTrials(PlayerController playerController, ReqSeasonTrials req) {
        execute(playerController, ctx -> ctx.send(seasonService.trials(ctx)));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_TRIAL_CHALLENGE)
    public void reqSeasonTrialChallenge(PlayerController playerController, ReqSeasonTrialChallenge req) {
        execute(playerController, ctx -> ctx.send(seasonService.trialChallenge(ctx, req.trialId)));
    }

    @Command(SeasonConstant.MsgBean.REQ_SEASON_TRIAL_PROGRESS)
    public void reqSeasonTrialProgress(PlayerController playerController, ReqSeasonTrialProgress req) {
        long playerId = playerController.playerId();
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx != null) {
            playerController.send(seasonService.trialProgress(ctx));
            return;
        }
        playerController.send(remoteSeasonTrialProgress(playerController));
    }

    public <T extends AbstractResponse> void execute(PlayerController pc, Consumer<SimPlayerContext> action) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(pc.playerId());
        if (ctx == null) {
            log.warn("获取ctx为空 playerId={}", pc.playerId());
            return;
        }
        action.accept(ctx);
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        if (gmOrders == null || gmOrders.length == 0 || !"season".equalsIgnoreCase(gmOrders[0])) {
            return new CommonResult<>(Code.NOT_FOUND);
        }
        SimPlayerContext ctx = simPlayerContextRegistry.getContext(playerController.playerId());
        if (ctx != null) {
            return seasonService.gmTime(ctx, gmOrders);
        }
        return remoteSeasonGm(playerController, gmOrders);
    }

    private ResSeasonMatch remoteSeasonMatch(PlayerController playerController, ReqSeasonMatch req) {
        long playerId = playerController.playerId();
        ClusterClient client = simNodeService.getSimClusterClient(playerId, playerController.ipAddress());
        if (client == null) {
            log.warn("赛季匹配失败，未找到玩家 sim 节点 playerId={}", playerId);
            return new ResSeasonMatch(Code.NOT_FOUND);
        }

        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            ResSeasonMatch response = toSimBridge.seasonMatch(playerId, req.gameType, req.stake);
            return response == null ? new ResSeasonMatch(Code.EXCEPTION) : response;
        } catch (Exception e) {
            log.error("远程赛季匹配异常 playerId={},gameType={},stake={}",
                    playerId, req.gameType, req.stake, e);
            return new ResSeasonMatch(Code.EXCEPTION);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    private ResSeasonTrialProgress remoteSeasonTrialProgress(PlayerController playerController) {
        long playerId = playerController.playerId();
        ClusterClient client = simNodeService.getSimClusterClient(playerId, playerController.ipAddress());
        if (client == null) {
            log.warn("获取赛季试炼进度失败，未找到玩家 sim 节点 playerId={}", playerId);
            return new ResSeasonTrialProgress(Code.NOT_FOUND);
        }

        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            ResSeasonTrialProgress response = toSimBridge.seasonTrialProgress(playerId);
            return response == null ? new ResSeasonTrialProgress(Code.EXCEPTION) : response;
        } catch (Exception e) {
            log.error("远程获取赛季试炼进度异常 playerId={}", playerId, e);
            return new ResSeasonTrialProgress(Code.EXCEPTION);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    private CommonResult<String> remoteSeasonGm(PlayerController playerController, String[] orders) {
        long playerId = playerController.playerId();
        ClusterClient client = simNodeService.getSimClusterClient(playerId, playerController.ipAddress());
        if (client == null) {
            log.warn("赛季 GM 执行失败，未找到玩家 sim 节点 playerId={}", playerId);
            return new CommonResult<>(Code.NOT_FOUND);
        }

        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            CommonResult<String> response = toSimBridge.seasonGm(playerId, orders);
            return response == null ? new CommonResult<>(Code.EXCEPTION) : response;
        } catch (Exception e) {
            log.error("远程执行赛季 GM 异常 playerId={}", playerId, e);
            return new CommonResult<>(Code.EXCEPTION);
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }
}
