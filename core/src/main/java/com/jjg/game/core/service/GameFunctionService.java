package com.jjg.game.core.service;

import com.jjg.game.common.constant.EFunctionType;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.ConditionManager;
import com.jjg.game.core.pb.NotifyOpenFunction;
import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ConditionCfg;
import com.jjg.game.sampledata.bean.GameFunctionCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
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
public class GameFunctionService implements GameEventListener, ConfigExcelChangeListener {

    /**
     * 条件检查服务
     */
    private final ConditionManager conditionManager;
    private final PlayerSessionService playerSessionService;

    private static final Logger log = LoggerFactory.getLogger(GameFunctionService.class);

    public GameFunctionService(ConditionManager conditionManager, PlayerSessionService playerSessionService) {
        this.conditionManager = conditionManager;
        this.playerSessionService = playerSessionService;
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
            if (checkGameFunctionOpen(player, functionCfg, false)) {
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
            if (checkGameFunctionOpen(player, gameFunctionCfg, false)) {
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
        return checkGameFunctionOpen(player, GameDataManager.getGameFunctionCfg(eFunctionType.getFunctionId()), true);
    }

    /**
     * 检查游戏功能开放，应该根据功能的整个协议蔟去拦截整个功能
     */
    public boolean checkGameFunctionOpen(Player player, EFunctionType eFunctionType, boolean notify) {
        return checkGameFunctionOpen(player, GameDataManager.getGameFunctionCfg(eFunctionType.getFunctionId()), notify);
    }

    /**
     * 检查游戏功能开放
     */
    public boolean checkGameFunctionOpen(Player player, GameFunctionCfg functionCfg, boolean notify) {
        if (functionCfg == null) {
            return false;
        }

        if (!functionCfg.getIsOpen()) {
            return false;
        }

        List<Integer> conditionTypes = functionCfg.getVipLevel();
        List<Integer> conditionCfg = new ArrayList<>(conditionTypes);
        // 检查是否触发成功
        boolean achievement = conditionManager.isAchievement(player, player.getLevel(), conditionCfg);
        if (!achievement && notify) {
            TipUtils.sendToastTip(player.getId(), 16030);
        }
        return achievement;
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        List<EGameEventType> needMonitorEvents = new ArrayList<>();
        if(this.gameTypeOfFuncCache.isEmpty()){
            loadConfig();
        }
        this.gameTypeOfFuncCache.forEach((k,v) -> needMonitorEvents.add(k));
        return needMonitorEvents;
    }


    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(GameFunctionCfg.EXCEL_NAME, this::loadConfig);
    }

    private void loadConfig(){
        Map<EGameEventType, List<GameFunctionCfg>> tmpGameTypeOfFuncCache = new HashMap<>();
        List<GameFunctionCfg> functionCfg = GameDataManager.getGameFunctionCfgList();
        for (GameFunctionCfg gameFunctionCfg : functionCfg) {
            if (!gameFunctionCfg.getIsOpen()) {
                continue;
            }
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
            tmpGameTypeOfFuncCache.computeIfAbsent(gameEventType, k -> new ArrayList<>()).add(gameFunctionCfg);
        }
        this.gameTypeOfFuncCache = tmpGameTypeOfFuncCache;
    }
}
