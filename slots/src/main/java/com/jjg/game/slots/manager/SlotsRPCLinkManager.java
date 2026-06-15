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
    public void notifySpin(SlotsPlayerGameData playerGameData, int gameType, int winTimes) {
        try {
            long playerId = playerGameData.getPlayerId();
            PlayerController playerController = playerGameData.getPlayerController();
            if (playerGameData.getSimClient() == null) {
                log.warn("获取sim节点为空 playerId = {}", playerId);
                return;
            }

            //检查该节点是否有效
            boolean changeNode = false;
            ClusterClient client = clusterSystem.getClusterByPath(playerGameData.getSimClient().marsNode.getNodePath());
            if (client == null) {
                client = simNodeService.getSimClusterClient(playerId, playerController.ipAddress());
                if (client == null) {
                    log.warn("获取sim节点为空 playerId = {}", playerId);
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
                rpcContext.asyncCall(() -> toSimBridge.onSlotsSpin(playerId, gameType, winTimes, finalChangeNode))
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                log.warn("sim道具掉落异步调用异常 playerId={},gameType={},winTimes={}", playerId, gameType, winTimes, throwable);
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
            log.error("", e);
        }
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
