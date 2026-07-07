package com.jjg.game.slots.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sim.bridge.ToSimBridge;
import com.jjg.game.sim.data.SlotsSpinResult;
import com.jjg.game.sim.data.SpinStatInfo;
import com.jjg.game.sim.data.VisitTrialSpinPermit;
import com.jjg.game.sim.service.SimNodeService;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.pb.NotifySimDropItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2026/6/11
 */
@Component
public class SlotsRPCLinkManager {
    private Logger log = LoggerFactory.getLogger(getClass());

    @ClusterRpcReference
    private ToSimBridge toSimBridge;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private SimNodeService simNodeService;

    /**
     * 通知sim节点
     * 非阻塞，异步通知
     *
     * @param playerGameData
     * @param gameType
     * @param winTimes
     */
    public void notifySpin(SlotsPlayerGameData playerGameData, int gameType, int winTimes,
                           SpinStatInfo statInfo, VisitTrialSpinPermit trialPermit) {
        notifySpin(playerGameData, gameType, winTimes, statInfo, trialPermit, 0);
    }

    private void notifySpin(SlotsPlayerGameData playerGameData, int gameType, int winTimes,
                            SpinStatInfo statInfo, VisitTrialSpinPermit trialPermit, int retryCount) {
        try {
            long playerId = playerGameData.getPlayerId();
            PlayerController playerController = playerGameData.getPlayerController();
            if (playerGameData.getSimClient() == null) {
                log.warn("获取sim节点为空 playerId = {}", playerId);
                if (retryCount == 0) {
                    cancelVisitTrialSpin(playerGameData, trialPermit);
                }
                return;
            }

            //检查该节点是否有效
            boolean changeNode = false;
            ClusterClient client = clusterSystem.getClusterByPath(playerGameData.getSimClient().marsNode.getNodePath());
            if (client == null) {
                client = simNodeService.getSimClusterClient(playerId, playerController.ipAddress());
                if (client == null) {
                    log.warn("获取sim节点为空 playerId = {}", playerId);
                    if (retryCount == 0) {
                        cancelVisitTrialSpin(playerGameData, trialPermit);
                    }
                    return;
                }
                playerGameData.setSimClient(client);
                changeNode = true;
            }

            GameRpcContext rpcContext = GameRpcContext.getContext();
            RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
            try {
                rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create().addClusterClient(client).setTryMillisPerClient(1000));
                boolean finalChangeNode = changeNode;
                rpcContext.asyncCall(() -> toSimBridge.onSlotsSpin(
                                playerId, gameType, winTimes, finalChangeNode, statInfo, trialPermit))
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                log.warn("sim道具掉落异步调用异常 playerId={},gameType={},winTimes={}", playerId, gameType, winTimes, throwable);
                                if (retryCount == 0) {
                                    //同一 permit 可安全重试：sim 侧 pending key 只允许一次结算。
                                    notifySpin(playerGameData, gameType, winTimes, statInfo, trialPermit, 1);
                                }
                                return;
                            }
                            try {
                                handleSpinResult(playerController, playerId, gameType, winTimes, result);
                            } catch (Exception e) {
                                log.error("处理sim道具掉落异步结果异常 playerId={},gameType={},winTimes={}", playerId, gameType, winTimes, e);
                            }
                        });
            } finally {
                rpcContext.setReqParameterBuilder(previousBuilder);
            }
        } catch (Exception e) {
            log.error("通知sim旋转异常 playerId={},gameType={}", playerGameData.getPlayerId(), gameType, e);
            if (retryCount == 0) {
                cancelVisitTrialSpin(playerGameData, trialPermit);
            }
        }
    }

    /**
     * 普通旋转不发 RPC；只有进入 slots 时绑定了客座会话才同步向 sim 申请许可。
     */
    public CommonResult<VisitTrialSpinPermit> prepareVisitTrialSpin(SlotsPlayerGameData playerGameData,
                                                                    int gameType) {
        VisitTrialSpinPermit normal = new VisitTrialSpinPermit();
        if (playerGameData.getVisitOwnerId() <= 0) {
            return new CommonResult<>(Code.SUCCESS, normal);
        }
        ClusterClient client = resolveSimClient(playerGameData);
        if (client == null) {
            return new CommonResult<>(Code.NOT_FOUND, normal);
        }
        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(1000));
            CommonResult<VisitTrialSpinPermit> result = toSimBridge.prepareVisitTrialSpin(
                    playerGameData.getPlayerId(), gameType);
            if (result == null) {
                return new CommonResult<>(Code.EXCEPTION, normal);
            }
            VisitTrialSpinPermit permit = result.data;
            if (result.success() && permit != null && permit.isTrial()) {
                if (permit.getOwnerId() == playerGameData.getVisitOwnerId()
                        && permit.getCasinoId() == playerGameData.getVisitCasinoId()) {
                    return result;
                }
                //sim 按当前会话授权了 permit, 但与本地绑定的房主/赌场不一致: 退回已扣的体力/次数, 再按会话失效处理
                cancelVisitTrialSpin(playerGameData, permit);
                return trialEnded(normal);
            }
            if (result.success()) {
                //sim 已无有效客座会话(过期/退出): 判定失效, 交由上层清理本地绑定
                log.warn("客座会话已失效, 拒绝本次旋转并请求清理 playerId={},localOwnerId={},localCasinoId={}",
                        playerGameData.getPlayerId(), playerGameData.getVisitOwnerId(),
                        playerGameData.getVisitCasinoId());
                return trialEnded(normal);
            }
            //sim 业务失败(体力/次数不足)或不可达: 会话可能仍有效, 透传原因、不触发清理
            return result;
        } finally {
            rpcContext.setReqParameterBuilder(previousBuilder);
        }
    }

    /**
     * 本地绑定了客座会话但 sim 侧已失效: 用 EXPIRE 标记上抛, 供上层识别并清理本地客座绑定。
     */
    private CommonResult<VisitTrialSpinPermit> trialEnded(VisitTrialSpinPermit normal) {
        return new CommonResult<>(Code.EXPIRE, normal);
    }

    public void cancelVisitTrialSpin(SlotsPlayerGameData playerGameData, VisitTrialSpinPermit permit) {
        if (permit == null || !permit.isTrial()) {
            return;
        }
        try {
            ClusterClient client = resolveSimClient(playerGameData);
            if (client == null) {
                return;
            }
            GameRpcContext rpcContext = GameRpcContext.getContext();
            RpcReqParameterBuilder previousBuilder = rpcContext.getReqParameterBuilder();
            try {
                rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                        .addClusterClient(client).setTryMillisPerClient(1000));
                toSimBridge.cancelVisitTrialSpin(playerGameData.getPlayerId(), permit);
            } finally {
                rpcContext.setReqParameterBuilder(previousBuilder);
            }
        } catch (Exception e) {
            log.error("取消客座赌局旋转异常 playerId={},permitId={}",
                    playerGameData.getPlayerId(), permit.getPermitId(), e);
        }
    }

    private ClusterClient resolveSimClient(SlotsPlayerGameData playerGameData) {
        ClusterClient cached = playerGameData.getSimClient();
        if (cached != null
                && clusterSystem.getClusterByPath(cached.marsNode.getNodePath()) != null) {
            return cached;
        }
        PlayerController playerController = playerGameData.getPlayerController();
        ClusterClient client = simNodeService.getSimClusterClient(playerGameData.getPlayerId(),
                playerController == null ? null : playerController.ipAddress());
        if (client != null) {
            playerGameData.setSimClient(client);
        }
        return client;
    }

    public int skillLevelUp(SlotsPlayerGameData slotsPlayerGameData, int skillId) {
        if (slotsPlayerGameData.getSimClient() == null) {
            return Code.FAIL;
        }

        GameRpcContext.getContext().withReqParameterBuilder(RpcReqParameterBuilder.create().addClusterClient(slotsPlayerGameData.getSimClient()).setTryMillisPerClient(1000));

        CommonResult<Map<Integer, Integer>> result = toSimBridge.skillLevelUp(slotsPlayerGameData.getPlayerId(), slotsPlayerGameData.getGameType(), skillId);
        if (!result.success()) {
            log.warn("技能设置失败 playerId={},gameTpye={},skillId={},code={}", slotsPlayerGameData.getPlayerId(), slotsPlayerGameData.getGameType(), skillId, result.code);
            return result.code;
        }
        slotsPlayerGameData.setSkillsMap(result.data);
        return result.code;
    }

    private void handleSpinResult(PlayerController playerController, long playerId, int gameType, int winTimes, CommonResult<SlotsSpinResult> result) {
        if (result == null || !result.success()) {
            log.warn("sim道具掉落失败 playerId={},gameType={},winTimes={},code={}", playerId, gameType, winTimes, result == null ? null : result.code);
            return;
        }

        Map<Integer, Long> newMap = new HashMap<>();
        for (Map.Entry en : result.data.getItemsMap().entrySet()) {
            newMap.put(Integer.parseInt(en.getKey().toString()), Long.parseLong(en.getValue().toString()));
        }
        NotifySimDropItem notify = new NotifySimDropItem();
        notify.itemMap = ItemUtils.buildItemInfo(newMap);
        notify.power = result.data.getPower();
        notify.researchPoint = result.data.getResearchPoints();
        playerController.send(notify);
        log.info("通知道具掉落 playerId={},notify={}", playerId, JSON.toJSONString(notify));
    }
}
