package com.jjg.game.sim.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.listener.OnSwitchNode;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sampledata.bean.VisitorLevelCfg;
import com.jjg.game.sampledata.bean.VisitorStarCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.controller.SimGameController;
import com.jjg.game.sim.data.CasinoData;
import com.jjg.game.sim.data.SimPlayerGameData;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.pb.res.ResSimEnterGame;
import com.jjg.game.sim.pb.res.ResSimGetSkills;
import com.jjg.game.sim.pb.res.ResSimUpgradeSkill;
import com.jjg.game.sim.pb.res.ResSyncGuestLocation;
import com.jjg.game.sim.pb.strcut.GameSkills;
import com.jjg.game.sim.service.SimPlayerGameDataService;
import com.jjg.game.sim.service.SimSkillService;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 模拟经营游戏管理器
 *
 * @author 11
 * @date 2026/5/15
 */
@Component
public class SimManager implements OnSwitchNode, ConfigExcelChangeListener {
    private final Logger log = LoggerFactory.getLogger(SimManager.class);

    @Autowired
    private SimPlayerGameDataService simPlayerGameDataService;
    @Autowired
    private SimSkillService simSkillService;

    //玩家状态检查任务句柄
    private volatile Timeout checkPlayerDataTimeout;

    //visitorLevel配置 guestId -> level -> cfg
    private Map<Integer, Map<Integer, VisitorLevelCfg>> visitorLevelCfgMap;
    //visitorStar配置 guestId -> star -> cfg
    private Map<Integer, Map<Integer, VisitorStarCfg>> visitorStarCfgMap;

    //储存玩家控制器
    protected Map<Long, SimGameController> gameControllerMap = new ConcurrentHashMap<>();

    /**
     * 初始化
     */
    public void init() {
        checkPlayerDataTimeout = WheelTimerUtil.scheduleAtFixedRate(this::checkPlayerDataTimer, 1, 2, TimeUnit.SECONDS);
    }

    /**
     * 玩家进入游戏
     *
     * @param playerController
     */
    public void onEnterGame(PlayerController playerController) {
        ResSimEnterGame res = new ResSimEnterGame(Code.SUCCESS);
        try {
            //初始化玩家数据
            SimGameController simGameController = createGameController(playerController);
            playerController.setScene(simGameController);
            //处理重连
            res.buildings = simGameController.handleReconnect(this.visitorLevelCfgMap, this.visitorStarCfgMap);
            res.guide = simGameController.getPlayerGameData().isGuide();

            log.info("玩家进入游戏 playerId={},res={}", playerController.playerId(), JSONObject.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 玩家退出游戏
     *
     * @param playerId
     */
    public void onExitGame(long playerId, ExitType exitType) {
        SimGameController gameController = this.gameControllerMap.get(playerId);
        if (gameController != null) {
            gameController.getPlayerGameData().setLastOfflineTime(System.currentTimeMillis());
        }
    }

    /**
     * 玩家完成新手引导
     *
     * @param playerId
     */
    public void onFinishGuide(long playerId) {
        try {
            SimGameController gameController = getGameController(playerId);
            if (gameController == null) {
                log.warn("玩家完成新手引导时获取 SimPlayerGameData 失败 playerId={}", playerId);
                return;
            }
            gameController.getPlayerGameData().setGuide(true);
            log.info("玩家完成新手引导 playerId={}", playerId);
        } catch (Exception e) {
            log.error("", e);
        }
    }


    /**
     * 玩家同步游客位置
     *
     * @param playerController
     * @param guestId
     * @param buildingId       建筑id
     */
    public void onGuestLcation(PlayerController playerController, int guestId, int buildingId, boolean enter) {
        ResSyncGuestLocation res = new ResSyncGuestLocation(Code.SUCCESS);
        try {
            SimGameController gameController = getGameController(playerController.playerId());
            if (gameController == null) {
                log.warn("同步游客位置: SimPlayerGameData 不存在 playerId={}", playerController.playerId());
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }

            res.code = gameController.guestLocation(guestId, buildingId, enter, this.visitorLevelCfgMap, this.visitorStarCfgMap);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 加载技能
     *
     * @param playerController
     */
    public void onLoadSlotsSkills(PlayerController playerController) {
        ResSimGetSkills res = new ResSimGetSkills(Code.SUCCESS);
        try {
            SimGameController gameController = getGameController(playerController.playerId());
            if (gameController == null) {
                log.warn("同步游客位置: SimPlayerGameData 不存在 playerId={}", playerController.playerId());
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }

            if (gameController.getSkillsDataMap() == null || gameController.getSkillsDataMap().isEmpty()) {
                List<SimSkillsData> tmpList = simSkillService.getAllSlostsSkills(playerController.playerId());
                if (tmpList != null && !tmpList.isEmpty()) {
                    Map<Integer, SimSkillsData> map = new HashMap<>();
                    for (SimSkillsData data : tmpList) {
                        map.put(data.getGameType(), data);
                    }
                    gameController.setSkillsDataMap(map);
                }
            }

            if (gameController.getSkillsDataMap() != null && !gameController.getSkillsDataMap().isEmpty()) {
                res.skills = new ArrayList<>();
                for (Map.Entry<Integer, SimSkillsData> en : gameController.getSkillsDataMap().entrySet()) {
                    SimSkillsData value = en.getValue();

                    GameSkills gameSkills = new GameSkills();
                    gameSkills.gameType = value.getGameType();
                    gameSkills.stake = value.getStakeList();

                    if (value.getSkillsMap() != null && !value.getSkillsMap().isEmpty()) {
                        gameSkills.skillInfos = new ArrayList<>();
                        for (Map.Entry<Integer, Integer> en2 : value.getSkillsMap().entrySet()) {
                            KVInfo kvInfo = new KVInfo();
                            kvInfo.key = en2.getKey();
                            kvInfo.value = en2.getValue();
                            gameSkills.skillInfos.add(kvInfo);
                        }
                    }
                    res.skills.add(gameSkills);
                }
            }

        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 升级技能
     *
     * @param playerController
     * @param skillPropId
     */
    public void onUpgradeSkill(PlayerController playerController, int gameType, int skillPropId) {
        ResSimUpgradeSkill res = new ResSimUpgradeSkill(Code.SUCCESS);
        try {
            SimGameController gameController = getGameController(playerController.playerId());
            if (gameController == null) {
                log.warn("升级技能失败: SimPlayerGameData 不存在 playerId={}", playerController.playerId());
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }

            SimSkillsData skillData = gameController.getSkillData(gameType);
            if (skillData == null) {
                log.warn("升级技能失败: simSkillsData 不存在 playerId={}", playerController.playerId());
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }

            res.code = simSkillService.upgradeSkill(gameController.getPlayerGameData(), skillData, skillPropId);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 初始化玩家数据
     *
     * @param playerController
     * @return
     */
    public SimGameController createGameController(PlayerController playerController) {
        SimGameController gameController = getGameController(playerController.playerId());
        if (gameController != null) {
            return gameController;
        }

        SimPlayerGameData gameData = getSimPlayerGameDataFromDB(playerController.playerId());
        if (gameData == null) {
            gameData = new SimPlayerGameData();
            gameData.setPlayerId(playerController.playerId());

            //初始化默认赌场
            CasinoData casinoData = new CasinoData();
            casinoData.setId(SimConstant.Common.DEFAULT_CASINO_ID);
            casinoData.setStatsId(SimConstant.Common.DEFAULT_CASINO_STATS_ID);
            CasinoStatsSheetCfg casinoCfg = GameDataManager.getCasinoStatsSheetCfg(SimConstant.Common.DEFAULT_CASINO_STATS_ID);
            casinoData.setProsperity(casinoCfg.getProsperity());
            casinoData.setResearchId(1);

            Map<Integer, CasinoData> casinoMap = new HashMap<>();
            casinoMap.put(casinoData.getId(), casinoData);
            gameData.setCasinoDataMap(casinoMap);
            gameData.setCurrentCasinoId(casinoData.getId());
        }

        gameController = new SimGameController();
        gameController.setPlayerController(playerController);
        gameController.setPlayerGameData(gameData);
        this.gameControllerMap.put(playerController.playerId(), gameController);
        return gameController;
    }

    public SimGameController getGameController(long playerId) {
        return this.gameControllerMap.get(playerId);
    }

    public SimPlayerGameData getSimPlayerGameDataFromDB(long playerId) {
        SimGameController gameController = this.gameControllerMap.get(playerId);
        if (gameController != null) {
            return gameController.getPlayerGameData();
        }
        return simPlayerGameDataService.getSimPlayerGameData(playerId);
    }

    /**
     * 服务器关闭
     */
    public void shutdown() {
        if (this.checkPlayerDataTimeout != null) {
            this.checkPlayerDataTimeout.cancel();
        }

        //玩家数据落库
        for (Map.Entry<Long, SimGameController> en : this.gameControllerMap.entrySet()) {
            SimGameController gc = en.getValue();
            this.simPlayerGameDataService.save(gc.getPlayerGameData());
            //技能数据落库
            if (gc.getSkillsDataMap() != null && !gc.getSkillsDataMap().isEmpty()) {
                for (SimSkillsData skill : gc.getSkillsDataMap().values()) {
                    this.simSkillService.save(skill);
                }
            }
        }
    }

    /**
     * 将SimPlayerGameData从内存移除，并保存到redis
     *
     * @param playerId
     */
    private void exitSaveData(long playerId) {
        SimGameController gameController = this.gameControllerMap.remove(playerId);
        if (gameController != null) {
            this.simPlayerGameDataService.save(gameController.getPlayerGameData());

            if (gameController.getSkillsDataMap() != null && !gameController.getSkillsDataMap().isEmpty()) {
                for (Map.Entry<Integer, SimSkillsData> en : gameController.getSkillsDataMap().entrySet()) {
                    this.simSkillService.save(en.getValue());
                }
            }
        }
    }

    /**
     * 定时检查玩家任务
     */
    private void checkPlayerDataTimer() {
        long now = System.currentTimeMillis();

        final Map<Integer, Map<Integer, VisitorStarCfg>> tmpStarMap = this.visitorStarCfgMap;
        final Map<Integer, Map<Integer, VisitorLevelCfg>> tmpLevelMap = this.visitorLevelCfgMap;
        for (Map.Entry<Long, SimGameController> en : this.gameControllerMap.entrySet()) {
            //分发到对应的线程
            PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(en.getKey(), 0, new BaseHandler<String>() {
                @Override
                public void action() {
                    en.getValue().generateGuestEvent(now, tmpLevelMap, tmpStarMap);
                }
            }.setHandlerParamWithSelf("sim check playerdata timer"));
        }
    }

    /**
     * 加载 VisitorLevel 配置
     */
    private void loadVisitorLevelConfig() {
        Map<Integer, Map<Integer, VisitorLevelCfg>> tmpVisitorLevelCfgMap = new HashMap<>();
        for (VisitorLevelCfg cfg : GameDataManager.getVisitorLevelCfgList()) {
            tmpVisitorLevelCfgMap.computeIfAbsent(cfg.getVisitor(), k -> new HashMap<>()).put(cfg.getLevel(), cfg);
        }
        this.visitorLevelCfgMap = tmpVisitorLevelCfgMap;
    }

    /**
     * 加载 VisitorStar 配置
     */
    private void loadVisitorStarConfig() {
        Map<Integer, Map<Integer, VisitorStarCfg>> tmpVisitorStarCfgMap = new HashMap<>();
        for (VisitorStarCfg cfg : GameDataManager.getVisitorStarCfgList()) {
            tmpVisitorStarCfgMap.computeIfAbsent(cfg.getVisitor(), k -> new HashMap<>()).put(cfg.getStarlevel(), cfg);
        }
        this.visitorStarCfgMap = tmpVisitorStarCfgMap;
    }

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(VisitorLevelCfg.EXCEL_NAME, this::loadVisitorLevelConfig);
        addInitSampleFileObserveWithCallBack(VisitorStarCfg.EXCEL_NAME, this::loadVisitorStarConfig);
    }

    @Override
    public void onSwitchNodeAction(PFSession pfSession) {
//        exitSaveData(pfSession.getPlayerId());
    }
}
