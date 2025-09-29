package com.jjg.game.core.service;

import com.jjg.game.common.constant.EFunctionType;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.base.condition.CheckerParam;
import com.jjg.game.core.base.condition.ConditionCheckService;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.NotifyOpenFunction;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import com.jjg.game.sampledata.bean.GameFunctionCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 游戏功能服务
 *
 * @author 2CL
 */
@Service
public class GameFunctionService implements GameEventListener {

    /**
     * 条件检查服务
     */
    @Autowired
    private ConditionCheckService conditionCheckService;
    @Autowired
    private PlayerSessionService playerSessionService;

    private static final Logger log = LoggerFactory.getLogger(GameFunctionService.class);

    /**
     * 游戏类型缓存功能配置
     */
    private final Map<EGameEventType, List<GameFunctionCfg>> gameTypeOfFuncCache = new HashMap<>();

    /**
     * 获取开放的功能ID列表
     */
    public List<Integer> getOpenedFuncIdList(Player player) {
        List<Integer> functionIdList = new ArrayList<>();
        for (GameFunctionCfg functionCfg : GameDataManager.getGameFunctionCfgList()) {
            if (checkGameFunctionOpen(player, functionCfg)) {
                functionIdList.add(functionCfg.getId());
            }
        }
        return functionIdList;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        List<GameFunctionCfg> gameFunctionCfgs = gameTypeOfFuncCache.get(gameEvent.getGameEventType());
        Player player = null;
        if (gameEvent instanceof PlayerEvent playerEvent) {
            player = playerEvent.getPlayer();
        }
        List<Integer> functionIdList = new ArrayList<>();
        for (GameFunctionCfg gameFunctionCfg : gameFunctionCfgs) {
            if (checkGameFunctionOpen(player, gameFunctionCfg)) {
                functionIdList.add(gameFunctionCfg.getId());
            }
        }
        if (player != null) {
            // 推送功能发生了变化
            NotifyOpenFunction notifyOpenFunction = new NotifyOpenFunction();
            notifyOpenFunction.functionIdList = functionIdList;
            PFSession session = playerSessionService.getSession(player.getId());
            session.send(notifyOpenFunction);
        }
    }


    /**
     * 检查游戏功能开放，应该根据功能的整个协议蔟去拦截整个功能
     */
    public boolean checkGameFunctionOpen(Player player, EFunctionType eFunctionType) {
        return checkGameFunctionOpen(player, GameDataManager.getGameFunctionCfg(eFunctionType.getFunctionId()));
    }

    /**
     * 检查游戏功能开放
     */
    public boolean checkGameFunctionOpen(Player player, GameFunctionCfg functionCfg) {
        if (functionCfg == null) {
            return false;
        }
        List<Integer> conditionTypes = functionCfg.getVipLevel();
        ConditionCfg conditionCfg = GameDataManager.getConditionCfg(conditionTypes.getFirst());
        if (conditionCfg == null) {
            return false;
        }
        List<Object> params =
            conditionTypes.subList(1, conditionTypes.size()).stream().map(a -> (Object) a).toList();
        List<CheckerParam> checkerParams =
            Collections.singletonList(
                new CheckerParam(new HashSet<>(conditionCfg.getConditionType()), params));
        // 检查是否触发成功
        return conditionCheckService.isTriggerComplete(player, conditionCfg, checkerParams);
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        List<EGameEventType> needMonitorEvents = new ArrayList<>();
        List<GameFunctionCfg> functionCfg = GameDataManager.getGameFunctionCfgList();
        for (GameFunctionCfg gameFunctionCfg : functionCfg) {
            // 条件类型
            List<Integer> conditionTypes = gameFunctionCfg.getVipLevel();
            ConditionCfg conditionCfg = GameDataManager.getConditionCfg(conditionTypes.getFirst());
            if (conditionCfg == null) {
                continue;
            }
            // 获取游戏事件类型
            EGameEventType gameEventType = EGameEventType.gameEventType(conditionCfg.getTriggerEventType());
            if (gameEventType == null) {
                log.error("条件表配置异常，配置的事件触发类型：{} 在游戏事件枚举中缺失", conditionCfg.getTriggerEventType());
                continue;
            }
            needMonitorEvents.add(gameEventType);
            gameTypeOfFuncCache.computeIfAbsent(gameEventType, k -> new ArrayList<>()).add(gameFunctionCfg);
        }
        return needMonitorEvents;
    }
}
