package com.jjg.game.core.service;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.EFunctionType;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.base.condition.ConditionNode;
import com.jjg.game.core.base.condition.ConditionParser;
import com.jjg.game.core.base.condition.conditionnode.AndNode;
import com.jjg.game.core.base.condition.conditionnode.AtomicNode;
import com.jjg.game.core.base.condition.conditionnode.NotNode;
import com.jjg.game.core.base.condition.conditionnode.OrNode;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.manager.ConditionManager;
import com.jjg.game.core.pb.NotifyOpenFunction;
import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GameFunctionCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 游戏功能服务
 *
 * @author 2CL
 */
@Service
public class GameFunctionService implements GameEventListener {

    private static final Logger log = LoggerFactory.getLogger(GameFunctionService.class);
    /**
     * 条件检查服务
     */
    private final ConditionManager conditionManager;
    private final ConditionParser conditionParser;
    private final ClusterSystem clusterSystem;

    public GameFunctionService(ConditionManager conditionManager, ConditionParser conditionParser, ClusterSystem clusterSystem) {
        this.conditionManager = conditionManager;
        this.conditionParser = conditionParser;
        this.clusterSystem = clusterSystem;
    }

    /**
     * 游戏类型缓存功能配置
     */
    private Map<EGameEventType, List<GameFunctionCfg>> gameTypeOfFuncCache = Collections.emptyMap();

    /**
     * 获取开放的功能ID列表
     */
    public List<Integer> getOpenedFuncIdList(Player player) {
        List<Integer> functionIdList = new ArrayList<>();
        for (GameFunctionCfg functionCfg : GameDataManager.getGameFunctionCfgList()) {
            if (checkGameFunctionOpen(player, functionCfg, false, false)) {
                functionIdList.add(functionCfg.getId());
            }
        }
        return functionIdList;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        Player player = null;
        if (gameEvent instanceof PlayerEvent playerEvent) {
            player = playerEvent.getPlayer();
        }
        if (player == null) {
            return;
        }
        //更新playerPlayerController
        PFSession session = clusterSystem.getSession(player.getId());
        if (session != null) {
            if (session.getReference() instanceof PlayerController playerController) {
                playerController.setPlayer(player);
            }
        }
        List<Integer> openedFuncIdList = getOpenedFuncIdList(player);
        // 推送功能发生了变化
        NotifyOpenFunction notifyOpenFunction = new NotifyOpenFunction();
        notifyOpenFunction.functionIdList = openedFuncIdList;
        if (session != null) {
            session.send(notifyOpenFunction);
        }
    }


    /**
     * 检查游戏功能开放，应该根据功能的整个协议蔟去拦截整个功能
     */
    public boolean checkGameFunctionOpen(Player player, EFunctionType eFunctionType) {
        return checkGameFunctionOpen(player, GameDataManager.getGameFunctionCfg(eFunctionType.getFunctionId()), true, true);
    }

    /**
     * 检查游戏功能开放，应该根据功能的整个协议蔟去拦截整个功能
     * 默认检查加入条件,并发送通知
     */
    public boolean checkGameFunctionOpen(PlayerController playerController, EFunctionType eFunctionType) {
        if (playerController == null) {
            return false;
        }
        Player player = playerController.getPlayer();
        return checkGameFunctionOpen(player, GameDataManager.getGameFunctionCfg(eFunctionType.getFunctionId()), true, true);
    }

    /**
     * 检查游戏功能开放，应该根据功能的整个协议蔟去拦截整个功能
     */
    public boolean checkGameFunctionOpen(EFunctionType eFunctionType) {
        return checkGameFunctionOpen(GameDataManager.getGameFunctionCfg(eFunctionType.getFunctionId()));
    }

    /**
     * 检查游戏功能开放，应该根据功能的整个协议蔟去拦截整个功能
     */
    public boolean checkGameFunctionOpen(Player player, EFunctionType eFunctionType, boolean join, boolean notify) {
        return checkGameFunctionOpen(player, GameDataManager.getGameFunctionCfg(eFunctionType.getFunctionId()), join, notify);
    }

    /**
     * 检查游戏功能开放
     */
    public boolean checkGameFunctionOpen(GameFunctionCfg functionCfg) {
        return functionCfg != null && functionCfg.getIsOpen();
    }

    /**
     * 检查游戏功能开放
     */
    public boolean checkGameFunctionOpen(Player player, GameFunctionCfg functionCfg, boolean join, boolean notify) {
        if (player == null || functionCfg == null) {
            return false;
        }

        if (!functionCfg.getIsOpen()) {
            return false;
        }
        String check = join ? functionCfg.getCondition() : functionCfg.getShowCondition();
        boolean achievement = conditionManager.isAchievement(player, "", check);
        // 检查是否触发成功
        if (!achievement && notify) {
            TipUtils.sendToastTip(player.getId(), 16030);
        }
        return achievement;
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        List<EGameEventType> needMonitorEvents = new ArrayList<>();
        if (this.gameTypeOfFuncCache.isEmpty()) {
            loadConfig();
        }
        this.gameTypeOfFuncCache.forEach((k, v) -> needMonitorEvents.add(k));
        return needMonitorEvents;
    }

    private void loadConfig() {
        Map<EGameEventType, List<GameFunctionCfg>> tmpGameTypeOfFuncCache = new HashMap<>();
        List<GameFunctionCfg> functionCfg = GameDataManager.getGameFunctionCfgList();
        for (GameFunctionCfg gameFunctionCfg : functionCfg) {
            if (!gameFunctionCfg.getIsOpen()) {
                continue;
            }
            ConditionNode node = conditionParser.parse(gameFunctionCfg.getShowCondition());
            analysisCondition(gameFunctionCfg, node, tmpGameTypeOfFuncCache);
        }
        this.gameTypeOfFuncCache = tmpGameTypeOfFuncCache;
    }

    private void analysisCondition(GameFunctionCfg gameFunctionCfg, ConditionNode node, Map<EGameEventType, List<GameFunctionCfg>> tmpGameTypeOfFuncCache) {
        switch (node) {
            case AtomicNode<?> atomicNode -> {
                String type = atomicNode.getHandler().type();
                // 获取游戏事件类型
                EGameEventType gameEventType = EGameEventType.gameEventType(type);
                if (gameEventType == null) {
                    log.error("条件表配置异常，配置的事件触发类型：{} 在游戏事件枚举中缺失", type);
                    return;
                }
                tmpGameTypeOfFuncCache.computeIfAbsent(gameEventType, k -> new ArrayList<>()).add(gameFunctionCfg);
            }
            case AndNode andNode ->
                    andNode.getChildren().forEach(child -> analysisCondition(gameFunctionCfg, child, tmpGameTypeOfFuncCache));
            case OrNode orNode ->
                    orNode.getChildren().forEach(child -> analysisCondition(gameFunctionCfg, child, tmpGameTypeOfFuncCache));
            case NotNode notNode -> analysisCondition(gameFunctionCfg, notNode.getChild(), tmpGameTypeOfFuncCache);
            default -> {
            }
        }
    }
}
