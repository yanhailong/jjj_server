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
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoListCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.CasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.data.SimPlayerGameData;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.event.SimEventBus;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.ResSimEnterGame;
import com.jjg.game.sim.pb.res.ResSimGetSkills;
import com.jjg.game.sim.pb.res.ResSimUpgradeSkill;
import com.jjg.game.sim.pb.res.ResSyncGuestLocation;
import com.jjg.game.sim.service.*;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 模拟经营游戏管理器
 *
 * @author 11
 * @date 2026/5/15
 */
@Component
public class SimManager implements OnSwitchNode {
    private final Logger log = LoggerFactory.getLogger(SimManager.class);

    @Autowired
    private SimPlayerGameDataService simPlayerGameDataService;
    @Autowired
    private SimCasinoDataService simCasinoDataService;
    @Autowired
    private SimSkillService simSkillService;
    @Autowired
    private SimNodeService simNodeService;
    @Autowired
    private SimGuestService guestService;
    @Autowired
    private SimReconnectService reconnectService;
    //所有玩家级定时回调 (Spring 自动注入全部实现, 按 order 排序)
    @Autowired
    private List<SimPlayerTickListener> tickHandlers;
    @Autowired
    private SimEventBus simEventBus;


    //玩家状态检查任务句柄
    private volatile Timeout checkPlayerDataTimeout;

    //储存玩家会话上下文
    protected Map<Long, SimPlayerContext> contextMap = new ConcurrentHashMap<>();

    /**
     * 初始化
     */
    public void init() {
        //按 order 排序 tick handlers (SimAutoSaveService 会以 MAX_VALUE 排在最后)
        this.tickHandlers.sort(Comparator.comparingInt(SimPlayerTickListener::order));
        this.simEventBus.init();
        checkPlayerDataTimeout = WheelTimerUtil.scheduleAtFixedRate(this::checkPlayerDataTimer, 1, 2, TimeUnit.SECONDS);
    }

    /**
     * 玩家进入游戏
     */
    public void onEnterGame(PlayerController playerController) {
        ResSimEnterGame res = new ResSimEnterGame(Code.SUCCESS);
        try {
            SimPlayerContext ctx = createContext(playerController);
            playerController.setScene(ctx);
            //处理重连
            res.buildings = reconnectService.handleReconnect(ctx);
            res.guide = ctx.getPlayerGameData().isGuide();

            ctx.getPlayerGameData().setLastOfflineTime(0);
            ctx.markDirty();
            log.info("玩家进入游戏 playerId={},res={}", playerController.playerId(), JSONObject.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 玩家退出游戏
     */
    public void onExitGame(long playerId, ExitType exitType) {
        SimPlayerContext ctx = this.contextMap.get(playerId);
        if (ctx != null) {
            ctx.getPlayerGameData().setLastOfflineTime(System.currentTimeMillis());
            ctx.markDirty();
        }
    }

    /**
     * 玩家完成新手引导
     */
    public void onFinishGuide(long playerId) {
        try {
            SimPlayerContext ctx = getContext(playerId);
            if (ctx == null) {
                log.warn("玩家完成新手引导时获取 SimPlayerContext 失败 playerId={}", playerId);
                return;
            }
            ctx.getPlayerGameData().setGuide(true);
            ctx.markDirty();
            log.info("玩家完成新手引导 playerId={}", playerId);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 同步游客位置
     */
    public void onGuestLcation(PlayerController playerController, int guestId, int buildingId, boolean enter) {
        ResSyncGuestLocation res = new ResSyncGuestLocation(Code.SUCCESS);
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                log.warn("同步游客位置: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }

            res.code = guestService.guestLocation(ctx, guestId, buildingId, enter);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 加载技能
     */
    public void onLoadSlotsSkills(PlayerController playerController) {
        ResSimGetSkills res = new ResSimGetSkills(Code.SUCCESS);
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                log.warn("加载技能: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }

            if (ctx.getSkillsDataMap() == null || ctx.getSkillsDataMap().isEmpty()) {
                List<SimSkillsData> tmpList = simSkillService.getAllSlostsSkills(playerController.playerId());
                if (tmpList != null && !tmpList.isEmpty()) {
                    Map<Integer, SimSkillsData> map = new HashMap<>();
                    for (SimSkillsData data : tmpList) {
                        map.put(data.getGameType(), data);
                    }
                    ctx.setSkillsDataMap(map);
                }
            }

            if (ctx.getSkillsDataMap() != null && !ctx.getSkillsDataMap().isEmpty()) {
                res.skills = new ArrayList<>();
                for (SimSkillsData value : ctx.getSkillsDataMap().values()) {
                    res.skills.add(SimPbConverter.toGameSkills(value));
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
     */
    public void onUpgradeSkill(PlayerController playerController, int gameType, int skillPropId) {
        ResSimUpgradeSkill res = new ResSimUpgradeSkill(Code.SUCCESS);
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                log.warn("升级技能失败: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }

            SimSkillsData skillData = ctx.getSkillData(gameType);
            if (skillData == null) {
                log.warn("升级技能失败: simSkillsData 不存在 playerId={}", playerController.playerId());
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }

            res.code = simSkillService.upgradeSkill(ctx, skillData, skillPropId);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 创建或获取玩家会话上下文
     * <p>
     * 加载顺序:
     * 1) load 玩家级 SimPlayerGameData (无则新建)
     * 2) load 玩家所有 CasinoData (一次 find by playerId)
     * 3) 按配置补齐解锁条件满足但未创建的赌场, 同步 markCasinoDirty 让它们落库
     */
    public SimPlayerContext createContext(PlayerController playerController) {
        SimPlayerContext ctx = getContext(playerController.playerId());
        if (ctx != null) {
            return ctx;
        }

        long playerId = playerController.playerId();
        boolean playerDirty = false;

        //load 玩家级数据
        SimPlayerGameData gameData = simPlayerGameDataService.getSimPlayerGameData(playerId);
        if (gameData == null) {
            gameData = new SimPlayerGameData();
            gameData.setPlayerId(playerId);
            playerDirty = true;
        }

        // load 玩家所有赌场
        List<CasinoData> casinos = simCasinoDataService.findByPlayerId(playerId);
        Map<Integer, CasinoData> casinoMap = new HashMap<>();
        for (CasinoData c : casinos) {
            casinoMap.put(c.getCasinoId(), c);
        }

        // 按 CasinoList 配置补齐解锁但未创建的赌场
        CasinoStatsSheetCfg defaultCasinoCfg = GameDataManager.getCasinoStatsSheetCfg(SimConstant.Common.DEFAULT_CASINO_STATS_ID);
        List<Integer> newlyUnlocked = new ArrayList<>();
        for (Map.Entry<Integer, CasinoListCfg> en : GameDataManager.getCasinoListCfgMap().entrySet()) {
            CasinoListCfg cfg = en.getValue();
            int casinoId = cfg.getId();
            //已解锁则跳过
            if (casinoMap.containsKey(casinoId)) {
                continue;
            }
            Map<Integer, Integer> condition = cfg.getCondition();
            boolean isDefault = condition == null || condition.isEmpty();
            if (!isDefault && !isCasinoUnlockConditionMet(condition, casinoMap)) {
                continue;
            }
            CasinoData casinoData = new CasinoData();
            casinoData.setPlayerId(playerId);
            casinoData.setCasinoId(casinoId);
            casinoData.setStatsId(SimConstant.Common.DEFAULT_CASINO_STATS_ID);
            casinoData.setProsperity(defaultCasinoCfg.getProsperity());
            //研究院作为建筑, level=1 由建筑系统实施时通过默认建筑解锁逻辑设置
            casinoMap.put(casinoId, casinoData);
            newlyUnlocked.add(casinoId);

            if (isDefault && gameData.getCurrentCasinoId() <= 0) {
                gameData.setCurrentCasinoId(casinoId);
                playerDirty = true;
            }
        }

        // 兜底: currentCasinoId 指向不存在的赌场时回退到任意已解锁赌场
        if (!casinoMap.containsKey(gameData.getCurrentCasinoId()) && !casinoMap.isEmpty()) {
            int fallback = casinoMap.keySet().iterator().next();
            gameData.setCurrentCasinoId(fallback);
            playerDirty = true;
            log.warn("玩家 currentCasinoId 指向不存在赌场, 回退 playerId={},fallback={}", playerId, fallback);
        }

        //装配 ctx
        ctx = new SimPlayerContext();
        ctx.setPlayerController(playerController);
        ctx.setPlayerGameData(gameData);
        ctx.setCasinoMap(casinoMap);
        this.contextMap.put(playerId, ctx);

        if (playerDirty) {
            ctx.markDirty();
        }
        for (int casinoId : newlyUnlocked) {
            ctx.markCasinoDirty(casinoId);
        }
        return ctx;
    }

    /**
     * 判断赌场解锁条件是否满足
     * condition: key = 依赖的 casinoId, value = 该赌场需达到的 CasinoStatsSheetCfg.level
     */
    private boolean isCasinoUnlockConditionMet(Map<Integer, Integer> condition, Map<Integer, CasinoData> casinoMap) {
        for (Map.Entry<Integer, Integer> en : condition.entrySet()) {
            CasinoData dependData = casinoMap.get(en.getKey());
            if (dependData == null) {
                return false;
            }
            CasinoStatsSheetCfg statsCfg = GameDataManager.getCasinoStatsSheetCfg(dependData.getStatsId());
            if (statsCfg == null || statsCfg.getLevel() < en.getValue()) {
                return false;
            }
        }
        return true;
    }

    public SimPlayerContext getContext(long playerId) {
        return this.contextMap.get(playerId);
    }

    /**
     * 服务器关闭: 收集所有 ctx 的玩家/赌场/技能数据落库
     */
    public void shutdown() {
        if (this.checkPlayerDataTimeout != null) {
            this.checkPlayerDataTimeout.cancel();
        }

        List<SimPlayerGameData> gameDataList = new ArrayList<>(this.contextMap.size());
        List<CasinoData> casinoList = new ArrayList<>();
        List<SimSkillsData> skillsDataList = new ArrayList<>();
        for (Map.Entry<Long, SimPlayerContext> en : this.contextMap.entrySet()) {
            SimPlayerContext ctx = en.getValue();
            if (ctx.getPlayerGameData() != null) {
                gameDataList.add(ctx.getPlayerGameData());
            }
            if (ctx.getCasinoMap() != null && !ctx.getCasinoMap().isEmpty()) {
                casinoList.addAll(ctx.getCasinoMap().values());
            }
            if (ctx.getSkillsDataMap() != null && !ctx.getSkillsDataMap().isEmpty()) {
                skillsDataList.addAll(ctx.getSkillsDataMap().values());
            }
        }

        //批量落库
        this.simPlayerGameDataService.saveAll(gameDataList);
        this.simCasinoDataService.saveAll(casinoList);
        this.simSkillService.saveAll(skillsDataList);

        //删除本节点上所有玩家的sim节点路由信息
        this.simNodeService.delete(this.contextMap.keySet());
    }

    /**
     * 将玩家数据从内存移除并保存
     */
    private void exitSaveData(long playerId) {
        SimPlayerContext ctx = this.contextMap.remove(playerId);
        if (ctx == null) {
            return;
        }
        this.simPlayerGameDataService.save(ctx.getPlayerGameData());
        if (ctx.getCasinoMap() != null && !ctx.getCasinoMap().isEmpty()) {
            this.simCasinoDataService.saveAll(ctx.getCasinoMap().values());
        }
        if (ctx.getSkillsDataMap() != null && !ctx.getSkillsDataMap().isEmpty()) {
            for (Map.Entry<Integer, SimSkillsData> en : ctx.getSkillsDataMap().entrySet()) {
                this.simSkillService.save(en.getValue());
            }
        }
    }

    /**
     * 定时检查玩家任务: 遍历所有 SimPlayerTickHandler 在玩家线程中执行
     */
    private void checkPlayerDataTimer() {
        long now = System.currentTimeMillis();

        for (Map.Entry<Long, SimPlayerContext> en : this.contextMap.entrySet()) {
            final SimPlayerContext ctx = en.getValue();
            //分发到对应的线程
            PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(en.getKey(), 0, new BaseHandler<String>() {
                @Override
                public void action() {
                    for (SimPlayerTickListener handler : tickHandlers) {
                        try {
                            handler.onTick(ctx, now);
                        } catch (Exception e) {
                            log.error("tick handler 异常 handler={},playerId={}", handler.getClass().getSimpleName(), ctx.playerId(), e);
                        }
                    }
                }
            }.setHandlerParamWithSelf("sim check playerdata timer"));
        }
    }

    @Override
    public void onSwitchNodeAction(PFSession pfSession) {
        onExitGame(pfSession.playerId, ExitType.INITIATIVE);
    }
}
