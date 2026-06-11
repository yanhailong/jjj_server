package com.jjg.game.slots.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.data.CommonResult;
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
            if (playerGameData.getSimClient() == null) {
                log.warn("获取sim节点为空 playerId = {}", playerGameData.getPlayerId());
                return;
            }

            //检查该节点是否有效
            boolean changeNode = false;
            ClusterClient client = clusterSystem.getClusterByPath(playerGameData.getSimClient().marsNode.getNodePath());
            if (client == null) {
                client = simNodeService.getSimClusterClient(playerGameData.getPlayerId(), playerGameData.getPlayerController().ipAddress());
                if (client == null) {
                    log.warn("获取sim节点为空 playerId = {}", playerGameData.getPlayerId());
                    return;
                }
                playerGameData.setSimClient(client);
                changeNode = true;
            }


            GameRpcContext.getContext().withReqParameterBuilder(RpcReqParameterBuilder.create().addClusterClient(client).setTryMillisPerClient(1000));
            CommonResult<SlotsSpinResult> result = toSimBridge.onSlotsSpin(playerGameData.getPlayerId(), gameType, winTimes, changeNode);
            if (!result.success()) {
                log.warn("sim道具掉落失败 playerId={},gameType={},winTimes={},code={}", playerGameData.getPlayerId(), gameType, winTimes, result.code);
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
            playerGameData.getPlayerController().send(notify);
            log.info("通知道具掉落 playerId={},notify={}", playerGameData.getPlayerId(), JSON.toJSONString(notify));
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
