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
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.dao.SimEmployeeDao;
import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.dao.SimSkillsDao;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.event.SimEventBus;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.*;
import com.jjg.game.sim.service.*;
import com.jjg.game.sim.service.tick.SimAutoSaveService;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
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
public class SimManager implements OnSwitchNode {
    private final Logger log = LoggerFactory.getLogger(SimManager.class);

    @Autowired
    private SimPlayerGameDao simPlayerGameDao;
    @Autowired
    private SimSkillService simSkillService;
    @Autowired
    private SimNodeService simNodeService;
    @Autowired
    private SimBuildingService buildingService;
    @Autowired
    private SimEmployeeService employeeService;

    //所有玩家级定时回调 (Spring 自动注入全部实现, 按 order 排序)
    @Autowired
    private List<SimPlayerTickListener> tickHandlers;
    @Autowired
    private SimAutoSaveService autoSaveService;
    @Autowired
    private SimEventBus simEventBus;

    //玩家状态检查任务句柄
    private volatile Timeout checkPlayerDataTimeout;

    //储存玩家会话上下文
    protected Map<Long, SimPlayerContext> contextMap = new ConcurrentHashMap<>();
    @Autowired
    private SimCasinoDao simCasinoDao;
    @Autowired
    private SimCasinoService simCasinoService;
    @Autowired
    private SimEmployeeDao simEmployeeDao;
    @Autowired
    private SimEmployeeService simEmployeeService;
    @Autowired
    private SimSkillsDao simSkillsDao;

    /**
     * 初始化
     */
    public void init() {
        this.autoSaveService.init();
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
            res.guide = ctx.getSimBaseData().isGuide();

            if (ctx.getCurrentCasino() != null && ctx.getCurrentCasino().getBuildingData() != null) {
                res.currentCasinoId = ctx.getCurrentCasino().getCasinoId();
                res.buildings = new ArrayList<>();
                for (Map.Entry<Integer, BuildingData> en : ctx.getCurrentCasino().getBuildingData().entrySet()) {
                    res.buildings.add(SimPbConverter.toBuildingInfo(en.getValue()));
                }
            }

            ctx.getSimBaseData().setLastOfflineTime(0);
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
            ctx.getSimBaseData().setLastOfflineTime(System.currentTimeMillis());
            exitSaveData(playerId);
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
            ctx.getSimBaseData().setGuide(true);
            log.info("玩家完成新手引导 playerId={}", playerId);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 解锁建筑
     */
    public void onUnlockBuilding(PlayerController playerController, int buildingId) {
        ResUnlockBuilding res = new ResUnlockBuilding(Code.SUCCESS);
        res.id = buildingId;
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                log.warn("解锁建筑: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }
            res.code = buildingService.unlockBuilding(ctx, buildingId);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 升级建筑 (启动 CD)
     */
    public void onUpgradeBuilding(PlayerController playerController, int buildingId) {
        ResUpgradeBuilding res = new ResUpgradeBuilding(Code.SUCCESS);
        res.id = buildingId;
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.warn("升级建筑: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                return;
            }

            res.code = buildingService.upgradeBuilding(ctx, buildingId);
            if (res.code == Code.SUCCESS) {
                BuildingData data = ctx.getCurrentCasino().findBuilding(buildingId);
                if (data != null) {
                    res.cdEndTime = data.getCdEndTime();
                }
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 完成建筑升级
     */
    public void onCompleteBuildingUpgrade(PlayerController playerController, int buildingId) {
        ResCompleteBuildingUpgrade res = new ResCompleteBuildingUpgrade(Code.SUCCESS);
        res.id = buildingId;
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.warn("完成建筑升级: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                return;
            }

            res.code = buildingService.completeBuildingUpgrade(ctx, buildingId);
            if (res.code == Code.SUCCESS) {
                BuildingData data = ctx.getCurrentCasino().findBuilding(buildingId);
                if (data != null) {
                    res.level = data.getLevel();
                }
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 清除建筑升级 CD
     */
    public void onClearBuildingCD(PlayerController playerController, int buildingId, Map<Integer, Long> costItems) {
        ResClearBuildingCD res = new ResClearBuildingCD(Code.SUCCESS);
        res.id = buildingId;
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.warn("清除建筑升级: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                return;
            }

            res.code = buildingService.clearBuildingCD(ctx, buildingId, costItems);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 招募雇员
     */
    public void onRecruitEmployee(PlayerController playerController, int employeeId) {
        ResRecruitEmployee res = new ResRecruitEmployee(Code.SUCCESS);
        res.employeeId = employeeId;
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.warn("招募雇员: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                return;
            }

            res.code = employeeService.recruitEmployee(ctx, employeeId);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 升级雇员
     */
    public void onUpgradeEmployee(PlayerController playerController, int employeeId) {
        ResUpgradeEmployee res = new ResUpgradeEmployee(Code.SUCCESS);
        res.employeeId = employeeId;
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.warn("升级雇员: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                return;
            }

            res.code = employeeService.upgradeEmployee(ctx, employeeId);
            SimEmployeeData ed = ctx.getEmployee(employeeId);
            if (ed != null) {
                res.level = ed.getLevel();
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 升星雇员
     */
    public void onStarUpEmployee(PlayerController playerController, int employeeId) {
        ResStarUpEmployee res = new ResStarUpEmployee(Code.SUCCESS);
        res.employeeId = employeeId;
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.warn("升星雇员: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                return;
            }

            res.code = employeeService.starUpEmployee(ctx, employeeId);
            SimEmployeeData ed = ctx.getEmployee(employeeId);
            if (ed != null) {
                res.star = ed.getStar();
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 任命主管
     */
    public void onAssignSupervisor(PlayerController playerController, int buildingId, int employeeId) {
        ResAssignSupervisor res = new ResAssignSupervisor(Code.SUCCESS);
        res.buildingType = buildingId;
        res.employeeId = employeeId;
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.warn("任命主管: SimPlayerContext 不存在 playerId={}", playerController.playerId());
                return;
            }

            res.code = employeeService.assignSupervisor(ctx, buildingId, employeeId);
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

            //加载技能数据
            simSkillService.loadSkillsData(ctx);

            res.skills = new ArrayList<>();

            for (SimSkillsData value : ctx.getSkillsDataMap().values()) {
                res.skills.add(SimPbConverter.toGameSkills(value));
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
     */
    public SimPlayerContext createContext(PlayerController playerController) {
        SimPlayerContext ctx = getContext(playerController.playerId());
        if (ctx != null) {
            return ctx;
        }
        //装配 ctx
        ctx = new SimPlayerContext();
        ctx.setPlayerController(playerController);

        long playerId = playerController.playerId();
        //加载玩家数据
        SimBaseData baseData = simPlayerGameDao.findById(playerId).orElse(null);
        if (baseData == null) {
            baseData = new SimBaseData();
            baseData.setPlayerId(playerId);
        }

        //加载场景数据
        simCasinoService.loadCasinoData(ctx, baseData);
        //加载雇员数据
        simEmployeeService.loadEmployeeData(ctx);

        ctx.setSimBaseData(baseData);
        this.contextMap.put(playerId, ctx);
        return ctx;
    }

    public SimPlayerContext getContext(long playerId) {
        return this.contextMap.get(playerId);
    }

    /**
     * 服务器关闭: 让所有玩家走一遍 onExitGame, 然后统一刷盘 + 释放缓存
     */
    public void shutdown() {
        if (this.checkPlayerDataTimeout != null) {
            this.checkPlayerDataTimeout.cancel();
        }
        //先等异步落库排空, 避免在途旧快照覆盖下面的同步全量落库
        this.autoSaveService.awaitPending();
        this.autoSaveService.destroy();

        //玩家级 SimPlayerGameData
        List<SimBaseData> gameDataList = new ArrayList<>(this.contextMap.size());
        List<SimCasinoData> simCasinoDataList = new ArrayList<>();
        List<SimEmployeeData> simEmployeeDataList = new ArrayList<>();
        List<SimSkillsData> skillDataList = new ArrayList<>();
        for (Map.Entry<Long, SimPlayerContext> en : this.contextMap.entrySet()) {
            try {
                SimPlayerContext ctx = en.getValue();
                onExitGame(en.getKey(), ExitType.DROPPED);

                gameDataList.add(ctx.getSimBaseData());
                simCasinoDataList.addAll(ctx.getCasinoMap().values());
                simEmployeeDataList.addAll(ctx.getEmployeeMap().values());
                skillDataList.addAll(ctx.getSkillsDataMap().values());
            } catch (Exception e) {
                log.error("shutdown onExitGame 异常 playerId={}", en.getKey(), e);
            }
        }

        simPlayerGameDao.saveAll(gameDataList);
        simCasinoDao.saveAll(simCasinoDataList);
        simEmployeeDao.saveAll(simEmployeeDataList);
        simSkillsDao.saveAll(skillDataList);
        //删除本节点上所有玩家的sim节点路由信息
        this.simNodeService.delete(this.contextMap.keySet());
    }

    /**
     * 单玩家退出落库 + 释放缓存 (供长时掉线/主动登出场景使用)
     */
    public void exitSaveData(long playerId) {
        SimPlayerContext ctx = this.contextMap.remove(playerId);
        if (ctx == null) {
            return;
        }
        //先等异步落库排空, 避免在途旧快照覆盖下面的同步全量落库
        autoSaveService.awaitPending();

        simPlayerGameDao.save(ctx.getSimBaseData());
        simCasinoDao.saveAll(ctx.getCasinoMap().values());
        simEmployeeDao.saveAll(ctx.getEmployeeMap().values());
        simSkillsDao.saveAll(ctx.getSkillsDataMap().values());
        //删除本节点上玩家的sim节点路由信息
        this.simNodeService.delete(playerId);
        log.info("保存玩家数据 playerId={}", playerId);
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
