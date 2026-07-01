package com.jjg.game.sim.manager;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.alliance.service.AllianceCacheService;
import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ExitType;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.dao.SimEmployeeDao;
import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.dao.SimSkillsDao;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.ResSimEnterGame;
import com.jjg.game.sim.pb.res.ResSimPlayerInfo;
import com.jjg.game.sim.service.*;
import com.jjg.game.sim.dao.SimTaskDao;
import com.jjg.game.sim.data.SimTaskData;
import com.jjg.game.sim.service.SimTaskService;
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
 * 管理一些跨service的操作和SimPlayerContext创建和销毁
 *
 * @author 11
 * @date 2026/5/15
 */
@Component
public class SimManager {
    private final Logger log = LoggerFactory.getLogger(SimManager.class);

    @Autowired
    private SimPlayerGameDao simPlayerGameDao;
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
    private SimEventBusManager simEventBusManager;

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
    private SimSkillsDao simSkillsDao;
    @Autowired
    private SimDropService simDropService;
    @Autowired
    private SimStatsService simStatsService;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private AllianceEventService allianceEventService;
    //联盟读缓存: 全系统读联盟数据的统一入口 (直接注入 AllianceCacheService 而非 AllianceService,
    //因为 AllianceService 反向依赖 SimManager, 直注会形成循环引用)
    @Autowired
    private AllianceCacheService allianceCacheService;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private SimTaskService simTaskService;
    @Autowired
    private SimTaskDao simTaskDao;
    @Autowired
    private SimVisitService simVisitService;


    /**
     * 初始化
     */
    public void init() {
        this.autoSaveService.init();
        //按 order 排序 tick handlers (SimAutoSaveService 会以 MAX_VALUE 排在最后)
        this.tickHandlers.sort(Comparator.comparingInt(SimPlayerTickListener::order));
        this.simEventBusManager.init();
        checkPlayerDataTimeout = WheelTimerUtil.scheduleAtFixedRate(this::checkPlayerDataTimer, 1, 2, TimeUnit.SECONDS);
    }

    /**
     * 玩家登录: 立即加载 sim 数据 (入 contextMap 后场景后台 tick 运行), 并结算离线收益。
     */
    public void onEnterSim(PlayerController playerController, boolean login) {
        try {
            SimPlayerContext ctx = createContext(playerController);
            //结算离线收益 (存 pendingOffline, 待进入 sim 界面时下发)
            if (login) {
                buildingService.settleOfflineReward(ctx);
                ctx.getSimBaseData().setLastOfflineTime(0);
            }
            log.info("玩家进入sim节点加载SimPlayerContext数据， playerId={}", playerController.playerId());
        } catch (Exception e) {
            log.error("玩家登录加载 sim 异常 playerId={}", playerController.playerId(), e);
        }
    }

    /**
     * 玩家进入 sim 界面: 复用登录时已加载的 ctx, 下发界面数据 (离线收益取登录时已结算的快照)
     */
    public void onEnterGame(PlayerController playerController) {
        ResSimEnterGame res = new ResSimEnterGame(Code.SUCCESS);
        try {
            SimPlayerContext ctx = getContext(playerController.playerId());
            if (ctx == null) {
                //兜底: 登录扩展点未触发时现场加载并结算离线收益
                ctx = createContext(playerController);
                buildingService.settleOfflineReward(ctx);
                ctx.getSimBaseData().setLastOfflineTime(0);
            }
            playerController.setScene(ctx);
            res.guide = ctx.getSimBaseData().isGuide();

            res.currentCasinoId = ctx.getCurrentCasino().getCasinoId();

            //添加建筑数据
            res.buildings = SimPbConverter.toBuildingInfos(ctx.getCurrentCasino());

            //添加主管信息
            res.managerEmployInfos = SimPbConverter.toManagerInfos(ctx.getCurrentCasino());

            //添加雇员信息
            if (ctx.getEmployeeMap() != null && !ctx.getEmployeeMap().isEmpty()) {
                res.employInfos = new ArrayList<>();
                for (Map.Entry<Integer, SimEmployeeData> en : ctx.getEmployeeMap().entrySet()) {
                    res.employInfos.add(SimPbConverter.toEmployeeInfo(en.getValue()));
                }
            }

            res.awareness = ctx.getCurrentCasino().getAwareness();
            res.power = ctx.getSimBaseData().getPower();

            res.researchPoint = ctx.getSimBaseData().findResearchPoint(SimConstant.ResearchPoint.NORMAL_TPYE);

            //已生成待领奖的购买游客 (断线重连补发, 客户端凭 uid 领奖)
            Map<String, PurchasedGuestData> purchasedGuestMap = ctx.getCurrentCasino().getPurchasedGuestMap();
            if (purchasedGuestMap != null && !purchasedGuestMap.isEmpty()) {
                res.purchasedGuests = new ArrayList<>(purchasedGuestMap.size());
                for (PurchasedGuestData data : purchasedGuestMap.values()) {
                    res.purchasedGuests.add(SimPbConverter.toGuestInfo(data));
                }
            }

            //离线收益已在登录时结算, 这里仅从快照构建下发
            res.offlineReward = buildingService.buildOfflineRewardPb(ctx.getPendingOffline());
            log.info("玩家进入游戏 playerId={},res={}", playerController.playerId(), JSONObject.toJSONString(res));
            playerController.send(res);
            return;
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 获取玩家信息
     *
     * @param playerController
     * @param targetPlayerId
     */
    public void simPlayerInfo(PlayerController playerController, long targetPlayerId) {
        ResSimPlayerInfo res = new ResSimPlayerInfo(Code.SUCCESS);
        try {
            if (targetPlayerId < 1) {
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.warn("获取玩家信息失败,参数错误 playerId={},targetPlayerId={}", playerController.playerId(), targetPlayerId);
                return;
            }

            Player targetPlayer;
            if (targetPlayerId == playerController.playerId()) {
                targetPlayer = playerController.getPlayer();
            } else {
                targetPlayer = corePlayerService.get(targetPlayerId);
            }

            if (targetPlayer == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.warn("获取玩家信息失败,未找到该玩家信息 playerId={},targetPlayerId={}", playerController.playerId(), targetPlayerId);
                return;
            }

            res.playerId = targetPlayer.getId();
            res.playerName = targetPlayer.getNickName();
            res.headImgId = targetPlayer.getHeadImgId();
            res.headFrameId = targetPlayer.getHeadFrameId();
            res.nationalId = targetPlayer.getNationalId();
            res.vipLevel = targetPlayer.getVipLevel();
            res.createTime = targetPlayer.getCreateTime();
            res.gender = targetPlayer.getGender();

            SimPlayerContext targetCtx = getContext(targetPlayerId);
            SimBaseData targetBaseData = targetCtx == null
                    ? simPlayerGameDao.findById(targetPlayerId).orElse(null)
                    : targetCtx.getSimBaseData();
            if (targetBaseData != null) {
                res.roleLevel = targetBaseData.getAllLevel();
            }
            SimTaskData targetTaskData = targetCtx == null
                    ? simTaskDao.findById(targetPlayerId).orElse(null)
                    : targetCtx.getSimTaskData();
            if (targetTaskData != null) {
                res.displayedMedalIds = new ArrayList<>(targetTaskData.getDisplayedMedalIds());
            }

            //联盟名称: 经读缓存统一入口取, 避开 SimManager <-> AllianceService 循环引用
            long allianceId = allianceCacheService.getAllianceId(targetPlayerId);
            AllianceData allianceData = allianceCacheService.getAlliance(allianceId);
            if (allianceData != null) {
                res.allianceName = allianceData.getName();
            }

            //已解锁场景id
            SimCasinoUnlock casinoUnlock = simCasinoService.getCasinoUnlock(targetPlayerId);
            if (casinoUnlock != null && casinoUnlock.getResearchLevelMap() != null && !casinoUnlock.getResearchLevelMap().isEmpty()) {
                res.unlockCasinoIds = casinoUnlock.getResearchLevelMap().keySet().stream().toList();
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 玩家退出游戏
     */
    public boolean onExitGame(long playerId, ExitType exitType) {
        SimPlayerContext ctx = this.contextMap.get(playerId);
        if (ctx != null) {
            ctx.getSimBaseData().setLastOfflineTime(System.currentTimeMillis());
            exitSaveData(playerId);
            return true;
        }
        return false;
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
     * 创建或获取玩家会话上下文
     */
    public SimPlayerContext createContext(PlayerController playerController) {
        SimPlayerContext ctx = createContextByPlayerId(playerController.playerId());
        if (ctx != null) {
            ctx.setPlayerController(playerController);
        }
        return ctx;
    }

    public SimPlayerContext createContextByPlayerId(long playerId) {
        SimPlayerContext ctx = getContext(playerId);
        if (ctx != null) {
            return ctx;
        }
        //装配 ctx
        ctx = new SimPlayerContext();
        ctx.setPlayerId(playerId);

        //加载玩家数据
        SimBaseData baseData = simPlayerGameDao.findById(playerId).orElse(null);
        if (baseData == null) {
            baseData = new SimBaseData();
            baseData.setPlayerId(playerId);
            baseData.setPower(10000);
        }
        ctx.setSimBaseData(baseData);
        //加载场景数据
        simCasinoService.loadCasinoData(ctx, baseData);
        migrateLegacyOperationStats(ctx);
        //加载雇员数据
        employeeService.loadEmployeeData(ctx);
        //加载主线/成就任务数据 (首登接取主线首节点+各成就组首节点)
        simTaskService.initTaskData(ctx);
        simTaskService.reconcileFinishedTaskCount(ctx);
        this.contextMap.put(playerId, ctx);
        simNodeService.save(playerId, clusterSystem.getNodePath());
        return ctx;
    }

    /**
     * 将旧版本按娱乐城保存的经营累计数据一次性汇总到玩家级数据。
     */
    private void migrateLegacyOperationStats(SimPlayerContext ctx) {
        SimBaseData baseData = ctx.getSimBaseData();
        if (baseData == null) {
            return;
        }
        List<SimCasinoData> casinos = simCasinoDao.findByPlayerId(ctx.playerId());
        if (casinos.isEmpty() && ctx.getCurrentCasino() != null) {
            casinos = List.of(ctx.getCurrentCasino());
        }
        int allLevel = 0;
        boolean migrateStats = !baseData.isOperationStatsMigrated();
        for (SimCasinoData casino : casinos) {
            allLevel += casino.getCasinoLevel();
            if (!migrateStats) {
                continue;
            }
            //旧 receptionCount 混入了普通游客及每个目的地交互，无法转换为“高级游客人数”，不迁移该字段
            baseData.addBusinessIncome(casino.getBusinessIncome());
            baseData.setWatchAdCount(baseData.getWatchAdCount() + casino.getWatchAdCount());
            baseData.setFinishedTaskCount(baseData.getFinishedTaskCount() + casino.getFinishedTaskCount());
            if (casino.getSlotStatsMap() != null) {
                for (Map.Entry<Integer, SlotGameStatsData> entry : casino.getSlotStatsMap().entrySet()) {
                    baseData.findOrCreateSlotStats(entry.getKey()).mergeFrom(entry.getValue());
                }
            }
        }
        if (allLevel > 0) {
            baseData.setAllLevel(allLevel);
        }
        if (migrateStats) {
            baseData.setOperationStatsMigrated(true);
        }
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
        List<SimTaskData> simTaskDataList = new ArrayList<>();
        for (Map.Entry<Long, SimPlayerContext> en : this.contextMap.entrySet()) {
            try {
                SimPlayerContext ctx = en.getValue();
                onExitGame(en.getKey(), ExitType.DROPPED);

                gameDataList.add(ctx.getSimBaseData());
                if (ctx.getCurrentCasino() != null) {
                    simCasinoDataList.add(ctx.getCurrentCasino());
                }
                simEmployeeDataList.addAll(ctx.getEmployeeMap().values());
                skillDataList.addAll(ctx.getSkillsDataMap().values());
                if (ctx.getSimTaskData() != null) {
                    simTaskDataList.add(ctx.getSimTaskData());
                }
            } catch (Exception e) {
                log.error("shutdown onExitGame 异常 playerId={}", en.getKey(), e);
            }
        }

        simPlayerGameDao.saveAll(gameDataList);
        simCasinoDao.saveAll(simCasinoDataList);
        simEmployeeDao.saveAll(simEmployeeDataList);
        simSkillsDao.saveAll(skillDataList);
        simTaskDao.saveAll(simTaskDataList);
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
        if (ctx.getCurrentCasino() != null) {
            simCasinoDao.save(ctx.getCurrentCasino());
        }
        simEmployeeDao.saveAll(ctx.getEmployeeMap().values());
        simSkillsDao.saveAll(ctx.getSkillsDataMap().values());
        if (ctx.getSimTaskData() != null) {
            simTaskDao.save(ctx.getSimTaskData());
        }
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

    public CommonResult<SlotsSpinResult> onSlotsSpin(long playerId, int gameType, int winTimes, boolean changeNode,
                                                    SpinStatInfo statInfo, VisitTrialSpinPermit trialPermit) {
        try {
            SimPlayerContext ctx = getContext(playerId);
            if (ctx == null) {
                if (changeNode) {
                    ctx = createContextByPlayerId(playerId);
                }

                if (ctx == null) {
                    //玩家未在 sim 在线: 跳过联动, 不影响 slots 旋转
                    log.warn("slots 联动跳过, 玩家未在 sim 在线 playerId={},gameType={},winTimes={}", playerId, gameType, winTimes);
                    return new CommonResult<>(Code.NOT_FOUND);
                }
            }

            boolean visitTrial = trialPermit != null && trialPermit.isTrial();
            //普通旋转沿用原语义：即使掉落失败也计入统计。试玩需要先通过 permit 幂等结算，避免 RPC 重试重复计数。
            if (!visitTrial) {
                simStatsService.recordSpin(ctx.getSimBaseData(), gameType, statInfo);
            }
            CommonResult<SlotsSpinResult> result = visitTrial
                    ? simVisitService.settleTrialSpin(ctx, gameType, statInfo, trialPermit)
                    : simDropService.onSpin(ctx, gameType, winTimes);
            if (!result.success()) {
                log.warn("slots 联动失败, onSpin执行失败 playerId={},gameType={},winTimes={},code={}", playerId, gameType, winTimes, result.code);
                return result;
            }
            if (visitTrial) {
                simStatsService.recordSpin(ctx.getSimBaseData(), gameType, statInfo);
            }

            //联盟联动: 消耗体力/中奖倍数 -> 任务进度 + 对决积分掉落 (内部吞异常, 不影响主流程)
            allianceEventService.onSpin(playerId, gameType, winTimes, SimConstant.Common.SPIN_COST_POWER, statInfo);

            //主线/成就任务联动: 旋转次数 + 累积投注 (内部吞异常, 不影响主流程)
            simTaskService.onSpin(ctx, gameType, statInfo);
            return result;
        } catch (Exception e) {
            log.error("", e);
            return new CommonResult<>(Code.EXCEPTION);
        }
    }

    public CommonResult<VisitTrialSpinPermit> prepareVisitTrialSpin(long playerId, int gameType) {
        try {
            SimPlayerContext ctx = getContext(playerId);
            if (ctx == null) {
                ctx = createContextByPlayerId(playerId);
            }
            if (ctx == null) {
                return new CommonResult<>(Code.NOT_FOUND);
            }
            return simVisitService.prepareTrialSpin(ctx, gameType);
        } catch (Exception e) {
            log.error("准备客座赌局旋转失败 playerId={},gameType={}", playerId, gameType, e);
            return new CommonResult<>(Code.EXCEPTION);
        }
    }

    public CommonResult<Boolean> cancelVisitTrialSpin(long playerId, VisitTrialSpinPermit permit) {
        try {
            SimPlayerContext ctx = getContext(playerId);
            if (ctx == null) {
                return new CommonResult<>(Code.NOT_FOUND, false);
            }
            boolean cancelled = simVisitService.cancelTrialSpin(ctx, permit);
            return new CommonResult<>(cancelled ? Code.SUCCESS : Code.FAIL, cancelled);
        } catch (Exception e) {
            log.error("取消客座赌局旋转失败 playerId={}", playerId, e);
            return new CommonResult<>(Code.EXCEPTION, false);
        }
    }
}
