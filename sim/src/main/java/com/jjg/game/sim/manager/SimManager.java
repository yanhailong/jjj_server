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
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.service.PlayerStatService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.VisitorQuestCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.*;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.ResFinishGuide;
import com.jjg.game.sim.pb.res.ResSimEnterGame;
import com.jjg.game.sim.pb.res.ResSimPlayerInfo;
import com.jjg.game.season.dao.SeasonPlayerDao;
import com.jjg.game.season.data.SeasonFreeSpinResult;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.service.SeasonEconomyService;
import com.jjg.game.season.service.SeasonFreeGameService;
import com.jjg.game.season.service.SeasonLifecycleService;
import com.jjg.game.season.service.SeasonService;
import com.jjg.game.sim.pb.struct.GuestInfo;
import com.jjg.game.sim.service.*;
import com.jjg.game.core.base.condition.numeric.GameConditionEvent;
import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
    @Autowired
    private SimSkillService skillService;

    //所有玩家级定时回调 (Spring 自动注入全部实现, 按 order 排序)
    @Autowired
    private List<SimPlayerTickListener> tickHandlers;
    @Autowired
    private SimAutoSaveService autoSaveService;
    @Autowired
    private SimEventBusManager simEventBusManager;
    @Autowired
    private SimGuestService simGuestService;

    //玩家状态检查任务句柄
    private volatile Timeout checkPlayerDataTimeout;
    private final AtomicLong rejectedTickTaskCount = new AtomicLong();
    private final AtomicLong nextTickRejectLogTime = new AtomicLong();
    private static final long TICK_REJECT_LOG_INTERVAL_MS = 60_000L;

    //登出落库在 sim-save-io 队列排队期间的暂存: 玩家在落库完成前重登/被 RPC 重建时
    //复用内存 ctx 或等待落库完成, 避免从库里读到未落地的旧数据
    private final Map<Long, PendingExitSave> pendingExitSaves = new ConcurrentHashMap<>();

    private static class PendingExitSave {
        final SimPlayerContext ctx;
        //单一归属权: 落库任务与重登复活谁先 CAS 成功谁处理该 ctx
        final AtomicBoolean claimed = new AtomicBoolean();
        final CountDownLatch done = new CountDownLatch(1);

        PendingExitSave(SimPlayerContext ctx) {
            this.ctx = ctx;
        }
    }

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
    private PlayerStatService playerStatService;
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
    private GameFunctionService gameFunctionService;
    @Autowired
    private SimTaskService simTaskService;
    @Autowired
    private SimTaskDao simTaskDao;
    @Autowired
    private SimCoopTaskService simCoopTaskService;
    @Autowired
    private SimCoopTaskDao simCoopTaskDao;
    @Autowired
    private SimVisitService simVisitService;
    @Autowired
    private SimMedalService simMedalService;
    @Autowired
    private SimPlayerContextRegistry simPlayerContextRegistry;
    @Autowired
    private SeasonPlayerDao seasonPlayerDao;
    @Autowired
    private com.jjg.game.activepass.service.ActivePassService activePassService;
    @Autowired
    private com.jjg.game.activepass.dao.ActivePassDao activePassDao;
    @Autowired
    private SeasonLifecycleService seasonLifecycleService;
    @Autowired
    private SeasonService seasonService;
    @Autowired
    private SeasonFreeGameService seasonFreeGameService;
    @Autowired
    private SeasonEconomyService economyService;
    @Autowired
    private SimGuideService guideService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private SimConfigCacheService simConfigCacheService;


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
            if (ctx.getLastGemEarningTime() == 0) {
                ctx.setLastGemEarningTime(System.currentTimeMillis());
            }
            //结算离线收益 (存 pendingOffline, 待进入 sim 界面时下发)
            if (login) {
                buildingService.settleOfflineReward(ctx);
                ctx.getSimBaseData().setLastOfflineTime(0);
            }
            ctx.getInCasino().set(true);
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
            SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerController.playerId());
            if (ctx == null) {
                //兜底: 登录扩展点未触发时现场加载并结算离线收益
                ctx = createContext(playerController);
                buildingService.settleOfflineReward(ctx);
                ctx.getSimBaseData().setLastOfflineTime(0);
            }
            playerController.setScene(ctx);
            // 条件3使用模拟经营所有场景等级之和 allLevel，进入大厅时补扫后台直改和旧玩家漏触发。
            int allLevel = ctx.getSimBaseData().getAllLevel();
            List<Integer> sceneLevelTriggeredGroups = guideService.triggerSceneTotalLevelReached(ctx, allLevel, false);
            // 条件4使用角色系统的玩家等级 Player.level；进入大厅时补扫，覆盖后台直改等级和旧玩家漏触发。
            int playerLevel = playerController.getPlayer() == null ? 0 : playerController.getPlayer().getLevel();
            List<Integer> levelTriggeredGroups = guideService.triggerPlayerLevelReached(ctx, playerLevel, false);
            // 条件7进入大厅时按当前开放功能补扫，覆盖离线期间或历史玩家漏触发。
            if (playerController.getPlayer() != null) {
                guideService.triggerFunctionsUnlocked(ctx, gameFunctionService.getOpenedFuncIdList(playerController.getPlayer()), false);
            }
            // PathName=1 代表模拟经营大厅；激活其他条件已满足但此前场景不符的等待组。
            guideService.triggerDeferredForPath(ctx, SimConstant.GuidePath.SIM_HALL, false);
            List<Integer> newPlayerTriggeredGroups = List.of();
            if (!ctx.getSimBaseData().isGuide()) {
                newPlayerTriggeredGroups = guideService.trigger(
                        ctx, SimConstant.GuideCondition.NEW_PLAYER, 0, false);
            }
            guideService.skipReconnectGuides(ctx);
            // 大厅只恢复 PathName 为空或 PathName=1 的引导，避免 SLOT 引导把玩家再次拉回游戏节点。
            res.guideGroupIds = guideService.pendingOpenGuideGroupIds(
                    ctx, SimConstant.GuidePath.SIM_HALL);
            res.completedGuideIds = ctx.getSimBaseData().completedGuideIds();
            res.guide = ctx.getSimBaseData().isGuide();
            log.info("进入模拟经营大厅检查等级引导 playerId={},playerLevel={},newGroups={},pendingGroups={}",
                    playerController.playerId(), playerLevel, levelTriggeredGroups, res.guideGroupIds);
            log.info("进入模拟经营大厅检查场景累计等级引导 playerId={},allLevel={},newGroups={}",
                    playerController.playerId(), allLevel, sceneLevelTriggeredGroups);
            log.info("进入模拟经营大厅检查新号引导 playerId={},guideCompleted={},newGroups={},pendingGroups={}",
                    playerController.playerId(), ctx.getSimBaseData().isGuide(),
                    newPlayerTriggeredGroups, res.guideGroupIds);

            res.currentCasinoId = ctx.getCurrentCasino().getCasinoId();

            //下发前先消费联盟助力抵扣, 否则展示的仍是未减少的升级CD
            buildingService.applyPendingSpeedup(ctx, ctx.getCurrentCasino(), System.currentTimeMillis());

            //添加建筑数据
            res.buildings = SimPbConverter.toBuildingInfos(ctx, simConfigCacheService);

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

            ItemCfg itemCfg = simConfigCacheService.getResearchPointItemCfg(0);
            if (itemCfg != null) {
                res.researchPoint = (int) playerPackService.getItemCount(ctx.playerId(), itemCfg.getId());
            }

            //已生成待领奖的购买游客 (断线重连补发, 客户端凭 uid 领奖)
            Map<String, PurchasedGuestData> purchasedGuestMap = ctx.getCurrentCasino().getPurchasedGuestMap();
            if (purchasedGuestMap != null && !purchasedGuestMap.isEmpty()) {
                res.purchasedGuests = new ArrayList<>();
                for (PurchasedGuestData data : purchasedGuestMap.values()) {
                    VisitorQuestCfg visitorQuestCfg = GameDataManager.getVisitorQuestCfg(data.getGuestId());
                    res.purchasedGuests.add(SimPbConverter.toGuestInfo(data, visitorQuestCfg));
                }
            }

            //检查是否有缓存的游客列表
            if (ctx.getCurrentCasino().cacheGuestInfoListSize() > 0) {
                if (res.purchasedGuests == null) {
                    res.purchasedGuests = new ArrayList<>();
                }

                Iterator<GuestInfo> it = ctx.getCurrentCasino().getCacheGuestInfoList().iterator();
                while (it.hasNext()) {
                    GuestInfo guestInfo = it.next();
                    simGuestService.handCacheGuest(ctx, guestInfo);
                    res.purchasedGuests.add(guestInfo);
                    it.remove();
                }

            }

            //离线收益已在登录时结算, 这里仅从快照构建下发
            res.offlineReward = buildingService.buildOfflineRewardPb(ctx.getPendingOffline());
            log.info("玩家进入游戏 playerId={},res={}", playerController.playerId(), JSONObject.toJSONString(res));
            playerController.send(res);
            return;
        } catch (Exception e) {
            log.error("玩家进入游戏异常 playerId={}", playerController.playerId(), e);
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

            SimPlayerContext targetCtx = this.simPlayerContextRegistry.getContext(targetPlayerId);
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
            Set<Integer> unlockedCasinoIds = casinoUnlock == null
                    ? Collections.emptySet() : casinoUnlock.findUnlockedCasinoIds();
            if (!unlockedCasinoIds.isEmpty()) {
                res.unlockCasinoIds = unlockedCasinoIds.stream().toList();
            }
        } catch (Exception e) {
            log.error("获取玩家信息异常 playerId={},targetPlayerId={}", playerController.playerId(), targetPlayerId, e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 玩家退出游戏
     */
    public boolean onExitGame(long playerId, ExitType exitType) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx != null) {
            long now = System.currentTimeMillis();
            seasonService.stopGemEarnings(ctx, now);
            ctx.getSimBaseData().setLastOfflineTime(now);
            //循环赛季对局中掉线: 记录离线时刻, 再次进入赛季时据此自动补完对局
            if (ctx.getSeasonPlayerData() != null) {
                ctx.getSeasonPlayerData().markMatchOffline(now);
            }

            if (ctx.getCurrentCasino().getCacheGuestInfoList() != null && !ctx.getCurrentCasino().getCacheGuestInfoList().isEmpty()) {
                //处理缓存的游客
                Iterator<GuestInfo> it = ctx.getCurrentCasino().getCacheGuestInfoList().iterator();
                while (it.hasNext()) {
                    GuestInfo guestInfo = it.next();
                    simGuestService.handCacheGuest(ctx, guestInfo);
                    it.remove();
                }
            }

            exitSaveData(playerId);
            return true;
        }
        return false;
    }

    public void exitCasino(long playerId) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx != null) {
            ctx.getInCasino().set(false);
        }
    }

    /**
     * 玩家完成一个新手引导步骤
     */
    public ResFinishGuide onFinishGuide(long playerId, int guideId) {
        return guideService.finish(simPlayerContextRegistry.getContext(playerId), guideId);
    }

    /**
     * 完成引导并暂存新触发的引导组，由消息处理器控制响应与通知的发送顺序。
     */
    public SimGuideService.FinishGuideResult onFinishGuideWithTriggers(long playerId, int guideId) {
        return guideService.finishWithTriggers(simPlayerContextRegistry.getContext(playerId), guideId);
    }

    /**
     * 跳过整个引导组，并返回可能由条件8触发的后续引导组。
     */
    public SimGuideService.SkipGuideGroupResult onSkipGuideGroup(long playerId, int guideGroupId) {
        return guideService.skipGroup(simPlayerContextRegistry.getContext(playerId), guideGroupId);
    }

    /**
     * 延迟发送完成引导组后由条件8触发的新引导通知。
     * 状态在完成请求中立即生效；这里只延迟客户端通知，避免与上一组结束表现挤在一起。
     */
    public void notifyGuideTriggersDelayed(long playerId, List<Integer> guideGroupIds) {
        if (guideGroupIds == null || guideGroupIds.isEmpty()) {
            return;
        }
        SimPlayerContext scheduledContext = simPlayerContextRegistry.getContext(playerId);
        if (scheduledContext == null) {
            return;
        }
        List<Integer> groupSnapshot = List.copyOf(guideGroupIds);
        WheelTimerUtil.schedule(() ->
                        PlayerExecutorGroupDisruptor.getDefaultExecutor().publishWithFallback(
                                playerId, 0, new BaseHandler<String>() {
                                    @Override
                                    public void action() {
                                        SimPlayerContext currentContext =
                                                simPlayerContextRegistry.getContext(playerId);
                                        if (currentContext != scheduledContext
                                                || currentContext.getPlayerController() == null
                                                || currentContext.getPlayerController().getScene() != currentContext) {
                                            log.info("跳过延迟的新手引导组通知 playerId={},groups={}",
                                                    playerId, groupSnapshot);
                                            return;
                                        }
                                        guideService.notifyTriggeredGroups(currentContext, groupSnapshot);
                                        log.info("延迟发送新手引导组通知 playerId={},delayMs={},groups={}",
                                                playerId,
                                                SimConstant.GuideTiming.GROUP_FINISH_NOTIFY_DELAY_MILLIS,
                                                groupSnapshot);
                                    }
                                }.setHandlerParamWithSelf("sim guide group finish delayed notify")),
                SimConstant.GuideTiming.GROUP_FINISH_NOTIFY_DELAY_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    /**
     * GM 强制完成指定引导步骤，不要求所属组已经触发。
     */
    public int onGmFinishGuides(long playerId, Collection<Integer> guideIds) {
        return guideService.forceFinishGuides(simPlayerContextRegistry.getContext(playerId), guideIds);
    }

    /**
     * GM 强制完成全部引导。
     */
    public int onGmFinishAllGuides(long playerId) {
        return guideService.forceFinishAll(simPlayerContextRegistry.getContext(playerId));
    }

    /**
     * 创建或获取玩家会话上下文
     */
    public SimPlayerContext createContext(PlayerController playerController) {
        SimPlayerContext ctx = createContextByPlayerId(playerController.playerId(), playerController);
        if (ctx != null) {
            ctx.setPlayerController(playerController);
        }
        return ctx;
    }

    public SimPlayerContext createContextByPlayerId(long playerId) {
        return createContextByPlayerId(playerId, null);
    }

    private SimPlayerContext createContextByPlayerId(long playerId, PlayerController playerController) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx != null) {
            if (playerController != null) {
                ctx.setPlayerController(playerController);
                simTaskService.onLogin(ctx);
            }
            return ctx;
        }
        //登出落库还在队列中: 复活内存 ctx 或等落库完成, 避免读到未落地的旧库数据
        ctx = tryReviveExitingContext(playerId);
        if (ctx != null) {
            if (playerController != null) {
                ctx.setPlayerController(playerController);
                simTaskService.onLogin(ctx);
            }
            return ctx;
        }
        //装配 ctx
        ctx = new SimPlayerContext();
        ctx.setPlayerId(playerId);
        if (playerController != null) {
            ctx.setPlayerController(playerController);
        }

        //加载玩家数据
        SimBaseData baseData = simPlayerGameDao.findById(playerId).orElse(null);
        if (baseData == null) {
            baseData = new SimBaseData();
            baseData.setPlayerId(playerId);
        } else {
            // 集中登录时把已有玩家的首次保存检查摊开到一个周期内；
            // 新玩家及加载阶段主动置 0 的迁移数据仍在下一次 tick 立即保存。
            autoSaveService.initializeAutoSaveSchedule(ctx, System.currentTimeMillis());
        }

        ctx.setSimBaseData(baseData);
        //加载技能 (须在加载场景数据之前: initUnlock 依赖已入内存的技能等级)
        skillService.loadSkillsData(ctx);
        //加载场景数据
        simCasinoService.loadCasinoData(ctx, baseData);
        //加载雇员数据
        employeeService.loadEmployeeData(ctx);
        SeasonPlayerData seasonData = seasonPlayerDao.findById(playerId).orElse(null);
        if (seasonData == null) {
            seasonData = new SeasonPlayerData();
            seasonData.setPlayerId(playerId);
        }
        ctx.setSeasonPlayerData(seasonData);
        seasonLifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        activePassService.initData(ctx);
        //赛季数据须先就绪，随后任务登录补报产生的事实事件才能同步推进通行证
        simTaskService.initTaskData(ctx);
        //徽章档位由已完成成就任务决定，必须在任务数据就绪后刷新
        simMedalService.refreshMedalBonusCache(ctx);
        //加载多人协作任务数据 (每日池懒重置)
        simCoopTaskService.initData(ctx);

        this.simPlayerContextRegistry.putContext(ctx);
        simNodeService.save(playerId, clusterSystem.getNodePath());
        return ctx;
    }

    /**
     * 服务器关闭: 排空旧快照后让所有玩家按正常退出流程结算、落库并释放缓存。
     */
    public void shutdown() {
        if (this.checkPlayerDataTimeout != null) {
            this.checkPlayerDataTimeout.cancel();
        }
        //先等异步落库排空, 避免在途旧快照覆盖后续退出落库
        this.autoSaveService.awaitPending();
        this.autoSaveService.destroy();

        List<Long> playerIds = new ArrayList<>(this.simPlayerContextRegistry.getContextMap().keySet());
        for (Long playerId : playerIds) {
            try {
                onExitGame(playerId, ExitType.DROPPED);
            } catch (Exception e) {
                log.error("shutdown onExitGame 异常 playerId={}", playerId, e);
            }
        }
    }

    /**
     * 单玩家退出落库 + 释放缓存 (供长时掉线/主动登出场景使用)。
     * 落库整体排入 sim-save-io 单线程队列: FIFO 天然保证在途异步旧快照先于本次全量落库执行,
     * 且不在调用方线程(玩家槽位/登出事件线程)上做屏障等待 + 同步 Mongo 写
     * (大规模掉线时那会把所有槽位线程串行在 IO 上, ring 填满后拒绝在线玩家请求)。
     */
    public void exitSaveData(long playerId) {
        SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
        if (ctx == null) {
            return;
        }
        //先挂 pending 再摘 registry: 保证任意时刻"不在 registry 的玩家必在 pending 中(或落库已完成)",
        //否则并发的 createContextByPlayerId 会在两步之间从库里读到未落地的旧数据
        PendingExitSave pending = new PendingExitSave(ctx);
        pendingExitSaves.put(playerId, pending);
        if (this.simPlayerContextRegistry.removeContext(playerId) == null) {
            //并发登出竞态: 对方已摘除并接管落库, 撤销本次 pending
            pendingExitSaves.remove(playerId, pending);
            return;
        }
        autoSaveService.enqueueTask(() -> {
            try {
                //玩家已重登复活 ctx: 放弃本次落库, 数据仍在内存, 由周期落库接管
                if (!pending.claimed.compareAndSet(false, true)) {
                    return;
                }
                simPlayerGameDao.save(ctx.getSimBaseData());
                if (ctx.getCurrentCasino() != null) {
                    simCasinoDao.save(ctx.getCurrentCasino());
                }
                simEmployeeDao.saveAll(ctx.getEmployeeMap().values());
                simSkillsDao.saveAll(ctx.getSkillsDataMap().values());
                if (ctx.getSimTaskData() != null) {
                    simTaskDao.save(ctx.getSimTaskData());
                }
                if (ctx.getSimCoopTaskData() != null) {
                    simCoopTaskDao.save(ctx.getSimCoopTaskData());
                }
                if (ctx.getSeasonPlayerData() != null) {
                    seasonPlayerDao.save(ctx.getSeasonPlayerData());
                }
                if (ctx.getActivePassData() != null) {
                    activePassDao.save(ctx.getActivePassData());
                }
                //落库完成后再删路由; 玩家可能在复活等待超时后已重建 ctx, 此时路由必须保留
                if (simPlayerContextRegistry.getContext(playerId) == null) {
                    this.simNodeService.delete(playerId);
                    //极小窗口补偿: 删除期间玩家恰好完成重建, 补回路由
                    if (simPlayerContextRegistry.getContext(playerId) != null) {
                        this.simNodeService.save(playerId, clusterSystem.getNodePath());
                    }
                }
                log.info("保存玩家数据 playerId={}", playerId);
            } finally {
                pendingExitSaves.remove(playerId, pending);
                pending.done.countDown();
            }
        });
    }

    /**
     * 玩家在登出落库尚未完成时重新进入: 直接复活内存 ctx (最新状态);
     * 若落库任务已开始执行, 则等它完成后返回 null 走正常加载, 保证读到最终快照。
     */
    private SimPlayerContext tryReviveExitingContext(long playerId) {
        PendingExitSave pending = pendingExitSaves.remove(playerId);
        if (pending == null) {
            return null;
        }
        if (pending.claimed.compareAndSet(false, true)) {
            SimPlayerContext ctx = pending.ctx;
            this.simPlayerContextRegistry.putContext(ctx);
            this.simNodeService.save(playerId, clusterSystem.getNodePath());
            log.info("重登复活待落库的 SimPlayerContext playerId={}", playerId);
            return ctx;
        }
        //落库任务正在执行: 等待完成, 随后从库加载
        try {
            if (!pending.done.await(10, TimeUnit.SECONDS)) {
                log.error("等待登出落库完成超时, 继续从库加载可能读到旧数据 playerId={}", playerId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * 定时检查玩家任务: 遍历所有 SimPlayerTickHandler 在玩家线程中执行
     */
    private void checkPlayerDataTimer() {
        long now = System.currentTimeMillis();

        for (Map.Entry<Long, SimPlayerContext> en : this.simPlayerContextRegistry.getContextMap().entrySet()) {
            final SimPlayerContext ctx = en.getValue();
            //分发到对应的线程
            boolean published = PlayerExecutorGroupDisruptor.getDefaultExecutor().tryPublish(en.getKey(), 0, new BaseHandler<String>() {
                @Override
                public void action() {
                    // ctx 在任务进入玩家槽位前已被捕获，排队期间可能因登出或重连失效。
                    if (!isCurrentContext(ctx)) {
                        return;
                    }
                    for (SimPlayerTickListener handler : tickHandlers) {
                        try {
                            handler.onTick(ctx, now);
                        } catch (Exception e) {
                            log.error("tick handler 异常 handler={},playerId={}", handler.getClass().getSimpleName(), ctx.playerId(), e);
                        }
                    }
                }
            }.setHandlerParamWithSelf("sim check playerdata timer"));
            if (!published) {
                recordRejectedTickTask(ctx.playerId(), now);
            }
        }
    }

    private boolean isCurrentContext(SimPlayerContext ctx) {
        return simPlayerContextRegistry.getContext(ctx.playerId()) == ctx;
    }

    private void recordRejectedTickTask(long playerId, long now) {
        long total = rejectedTickTaskCount.incrementAndGet();
        long nextLogTime = nextTickRejectLogTime.get();
        if (now >= nextLogTime
                && nextTickRejectLogTime.compareAndSet(nextLogTime, now + TICK_REJECT_LOG_INTERVAL_MS)) {
            log.warn("sim tick task rejected playerId={},totalRejected={}", playerId, total);
        }
    }

    public long getRejectedTickTaskCount() {
        return rejectedTickTaskCount.get();
    }

    public CommonResult<SlotsSpinResult> onSlotsSpin(long playerId, int gameType, int winTimes, boolean changeNode,
                                                     SpinStatInfo statInfo, VisitTrialSpinPermit trialPermit,
                                                     int enterType) {
        SimPlayerContext ctx = null;
        long spinId = statInfo == null ? 0 : statInfo.getSpinId();
        try {
            log.warn("onSlotsSpin 方法playerId={},gameType={},winTimes={},enterType={},statInfo={},trialPermit={}",
                    playerId, gameType, winTimes, enterType,
                    statInfo != null ? JSONObject.toJSONString(statInfo) : "null",
                    trialPermit != null ? JSONObject.toJSONString(trialPermit) : "null");
            boolean visitTrial = trialPermit != null && trialPermit.isTrial();
            ctx = this.simPlayerContextRegistry.getContext(playerId);
            if (ctx == null) {
                ctx = createContextByPlayerId(playerId);
                if (ctx == null) {
                    //玩家未在 sim 在线: 跳过联动, 不影响 slots 旋转
                    log.warn("slots 联动跳过, 玩家未在 sim 在线 playerId={},gameType={},winTimes={}", playerId, gameType, winTimes);
                    return new CommonResult<>(Code.NOT_FOUND);
                }
            }

            CommonResult<SlotsSpinResult> cachedResult = ctx.spinResult(spinId);
            if (cachedResult != null) {
                log.warn("slots 联动重复投递, 返回原结果 playerId={},gameType={},spinId={}", playerId, gameType, spinId);
                return cachedResult;
            }

            boolean normalMode = statInfo != null && statInfo.getSpecialModes() != null
                    && statInfo.getSpecialModes().contains(1);
            //仅普通模式且普通入口消耗体力，与 SimDropService 的实际扣能条件一致。
            int configuredSpinCostPower = normalMode && enterType <= 0 ? SimConstant.Common.SPIN_COST_POWER : 0;
            //普通旋转沿用原语义：即使掉落失败也计入统计。试玩需要先通过 permit 幂等结算，避免 RPC 重试重复计数。
            if (!visitTrial) {
                simStatsService.recordSpin(ctx.getSimBaseData(), gameType, statInfo);
            }
            CommonResult<SlotsSpinResult> result = visitTrial
                    ? simVisitService.settleTrialSpin(ctx, gameType, statInfo, trialPermit)
                    : simDropService.onSpin(ctx, gameType, winTimes, statInfo, enterType);
            int actualSpinCostPower = result.success() ? configuredSpinCostPower : 0;
            //须在掉落结算后构造: 本次到账的道具要计入 itemGains, 12202 等按道具计数的条件才能推进
            GameConditionEvent conditionEvent = SimConditionEventFactory.fromSpin(
                    gameType, winTimes, actualSpinCostPower, statInfo,
                    result.data == null ? null : result.data.getItemsMap(), actualSpinCostPower > 0,
                    enterType == EnterGameType.SEASON.getValue());
            if (!result.success()) {
//                log.warn("slots 联动失败, onSpin执行失败 playerId={},gameType={},winTimes={},code={}", playerId, gameType, winTimes, result.code);
                //试玩失败意味着 permit 已失效(重复投递等), 本次上报不可信, 不推进任何进度
                if (visitTrial) {
                    ctx.recordSpinResult(spinId, result);
                    return result;
                }
            } else if (visitTrial) {
                simStatsService.recordSpin(ctx.getSimBaseData(), gameType, statInfo);
            }

            //真实旋转已发生: 体力不足/掉落失败都不影响下面的进度推进, 任务只认旋转本身这一事实
            //联盟联动: 消耗体力/中奖倍数 -> 任务进度 + 对决积分掉落 (内部吞异常, 不影响主流程)
            allianceEventService.onSpin(playerId, actualSpinCostPower, conditionEvent);

            boolean visitSlotSpin = enterType == EnterGameType.VISIT.getValue();
            if (visitSlotSpin) {
                playerStatService.recordVisitSlotSpin(playerId, gameType);
            }
            //主线/成就任务联动: 变化随现有 RPC 结果返回，由持有会话的 slots 节点通知客户端
            var taskUpdates = simTaskService.collectConditionEventUpdates(ctx, conditionEvent);
            if (visitSlotSpin) {
                var visitUpdates = simTaskService.collectConditionEventUpdates(ctx,
                        new ActionConditionEvent(ActionConditionEvent.Type.VISIT_SLOT_SPIN,
                                gameType, 0, 0, 1, 0, false));
                if (!visitUpdates.isEmpty()) {
                    taskUpdates = new ArrayList<>(taskUpdates);
                    taskUpdates.addAll(visitUpdates);
                }
            }
            if (!taskUpdates.isEmpty()) {
                if (result.data == null) {
                    result.data = new SlotsSpinResult();
                }
                result.data.setTaskUpdates(taskUpdates);
            }
            //赛季联动: 宝石掉落/试炼窗口/对局结算 (内部吞异常, 不影响主流程)
            if (!visitTrial) {
                Map<Integer, Long> gemGains = seasonService.onSpin(ctx, gameType, statInfo,
                        statInfo == null ? null : conditionEvent, enterType);
                //宝石入账在 sim 侧, 客户端只认 rpc 返回的这一份掉落, 故并回本次结果一起下发
                if (result.data != null) {
                    result.data.mergeItems(gemGains);
                }
            }
            // 旋转期间完成的任务可能使 PathName=2 的引导进入待触发状态。
            // 玩家此时已经在 SLOT 场景，无需等待下一次 enterGame；由持有当前会话的 SLOT 节点下发通知。
            List<Integer> slotGuideGroups = guideService.triggerDeferredForPath(
                    ctx, SimConstant.GuidePath.SLOTS, false);
            if (!slotGuideGroups.isEmpty()) {
                if (result.data == null) {
                    result.data = new SlotsSpinResult();
                }
                result.data.setGuideGroupIds(slotGuideGroups);
            }
            ctx.recordSpinResult(spinId, result);
            return result;
        } catch (Exception e) {
            log.error("slots 联动处理异常 playerId={},gameType={},winTimes={},spinId={}",
                    playerId, gameType, winTimes, spinId, e);
            CommonResult<SlotsSpinResult> result = new CommonResult<>(Code.EXCEPTION);
            if (ctx != null) {
                ctx.recordSpinResult(spinId, result);
            }
            return result;
        }
    }

    public CommonResult<SeasonFreeSpinResult> useSeasonFreeSpin(long playerId, int gameType) {
        try {
            SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
            if (ctx == null) {
                ctx = createContextByPlayerId(playerId);
            }
            if (ctx == null) {
                return new CommonResult<>(Code.NOT_FOUND);
            }
            return seasonFreeGameService.useFreeGame(ctx, gameType);
        } catch (Exception e) {
            log.error("赛季免费局消耗失败 playerId={},gameType={}", playerId, gameType, e);
            return new CommonResult<>(Code.EXCEPTION);
        }
    }

    public CommonResult<SlotsEntrySessionData> getSlotsSessionData(long skillOwnerId, int casinoId,
                                                                   int gameType, boolean seasonEntry) {
        try {
            SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(skillOwnerId);
            if (seasonEntry && ctx == null) {
                ctx = createContextByPlayerId(skillOwnerId);
            }
            if (seasonEntry && (ctx == null || ctx.getSeasonPlayerData() == null)) {
                return new CommonResult<>(Code.NOT_FOUND);
            }
            SlotsEntrySessionData sessionData = new SlotsEntrySessionData();
            if (seasonEntry) {
                sessionData.setSeasonData(seasonService.slotsSessionData(ctx, gameType));
            } else {
                SimSkillsData skillsData = ctx == null
                        ? skillService.getSkillDataByGameType(skillOwnerId, gameType) : ctx.getSkillData(gameType);
                Map<Integer, SkillDetailData> skillDetails = skillsData == null ? null : skillsData.getSkillsMap();
                if (skillDetails != null) {
                    Map<Integer, Integer> researchSkills = new HashMap<>(skillDetails.size());
                    for (Map.Entry<Integer, SkillDetailData> en : skillDetails.entrySet()) {
                        researchSkills.put(en.getKey(), en.getValue().getLevel());
                    }
                    sessionData.setResearchSkills(researchSkills);
                }
                SimSkillsData globalSkills = ctx == null
                        ? skillService.getSkillDataByGameType(skillOwnerId, 0) : ctx.getSkillData(0);
                sessionData.setTogetherPlaySkillEffect(skillService.togetherPlaySkillEffect(globalSkills));
            }
            SimCasinoData currentCasino = ctx == null ? null : ctx.getCurrentCasino();
            int skillCasinoId = casinoId;
            if (skillCasinoId <= 0 && currentCasino == null) {
                SimBaseData baseData = simPlayerGameDao.findById(skillOwnerId).orElse(null);
                skillCasinoId = baseData == null ? 0 : baseData.getCurrentCasinoId();
            }
            simGuestService.addVisitorBondsSkillBonus(
                    skillOwnerId, currentCasino, skillCasinoId, gameType, sessionData.getVisitorBondsEffect());
            return new CommonResult<>(Code.SUCCESS, sessionData);
        } catch (Exception e) {
            log.error("获取 slots 进场快照失败 skillOwnerId={},casinoId={},gameType={},seasonEntry={}",
                    skillOwnerId, casinoId, gameType, seasonEntry, e);
            return new CommonResult<>(Code.EXCEPTION);
        }
    }

    /**
     * 保留给滚动升级期间尚未切换进场快照协议的旧 slots 节点。
     */
    @Deprecated
    public CommonResult<Long> getSeasonCoin(long playerId) {
        try {
            SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
            if (ctx == null) {
                ctx = createContextByPlayerId(playerId);
            }
            if (ctx == null || ctx.getSeasonPlayerData() == null) {
                return new CommonResult<>(Code.NOT_FOUND);
            }
            return new CommonResult<>(Code.SUCCESS, ctx.getSeasonPlayerData().getSeasonCoin());
        } catch (Exception e) {
            log.error("赛季币余额查询失败 playerId={}", playerId, e);
            return new CommonResult<>(Code.EXCEPTION);
        }
    }

    public CommonResult<Long> deductSeasonCoin(long playerId, long amount, long transactionId) {
        try {
            SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
            if (ctx == null) {
                ctx = createContextByPlayerId(playerId);
            }
            if (ctx == null || ctx.getSeasonPlayerData() == null) {
                return new CommonResult<>(Code.NOT_FOUND);
            }
            //幂等: 超时重试同一 txnId 直接返回已提交的余额, 不重复扣
            Long done = ctx.seasonTxnResult(transactionId);
            if (done != null) {
                return new CommonResult<>(Code.SUCCESS, done);
            }
            long balance = economyService.spendForSlots(ctx.getSeasonPlayerData(), amount);
            if (balance < 0) {
                return new CommonResult<>(Code.NOT_ENOUGH);
            }
            //先记账本再落库: 内存态为准, 落库 best-effort (异常不回滚内存、不影响返回)
            ctx.recordSeasonTxn(transactionId, balance);
            bestEffortSave(ctx.getSeasonPlayerData());
            return new CommonResult<>(Code.SUCCESS, balance);
        } catch (Exception e) {
            log.error("赛季币扣除失败 playerId={},amount={},txnId={}", playerId, amount, transactionId, e);
            return new CommonResult<>(Code.EXCEPTION);
        }
    }

    public CommonResult<Long> addSeasonCoin(long playerId, long amount, long transactionId) {
        try {
            SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
            if (ctx == null) {
                ctx = createContextByPlayerId(playerId);
            }
            if (ctx == null || ctx.getSeasonPlayerData() == null) {
                return new CommonResult<>(Code.NOT_FOUND);
            }
            //幂等: 超时重试同一 txnId 直接返回已提交的余额, 不重复发
            Long done = ctx.seasonTxnResult(transactionId);
            if (done != null) {
                return new CommonResult<>(Code.SUCCESS, done);
            }
            long balance = economyService.addBalance(ctx.getSeasonPlayerData(), amount);
            ctx.recordSeasonTxn(transactionId, balance);
            bestEffortSave(ctx.getSeasonPlayerData());
            return new CommonResult<>(Code.SUCCESS, balance);
        } catch (Exception e) {
            log.error("赛季币增加失败 playerId={},amount={},txnId={}", playerId, amount, transactionId, e);
            return new CommonResult<>(Code.EXCEPTION);
        }
    }

    /**
     * 内存态已变更后的 best-effort 落库: 落库异常仅记录, 不回滚内存、不影响接口返回
     * (sim 内存为权威态, 周期性全量落库会兜底持久化)。
     */
    private void bestEffortSave(com.jjg.game.season.data.SeasonPlayerData data) {
        try {
            autoSaveService.enqueueSave(data);
        } catch (Exception e) {
            log.error("赛季币变更落库入队失败(内存已提交, 待周期落库兜底) playerId={}", data.getPlayerId(), e);
        }
    }

    public CommonResult<VisitTrialSpinPermit> prepareVisitTrialSpin(long playerId, int gameType, boolean freeMode) {
        try {
            SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
            if (ctx == null) {
                ctx = createContextByPlayerId(playerId);
            }
            if (ctx == null) {
                return new CommonResult<>(Code.NOT_FOUND);
            }
            return simVisitService.prepareTrialSpin(ctx, gameType, freeMode);
        } catch (Exception e) {
            log.error("准备客座赌局旋转失败 playerId={},gameType={}", playerId, gameType, e);
            return new CommonResult<>(Code.EXCEPTION);
        }
    }

    public CommonResult<Boolean> cancelVisitTrialSpin(long playerId, VisitTrialSpinPermit permit) {
        try {
            SimPlayerContext ctx = this.simPlayerContextRegistry.getContext(playerId);
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
