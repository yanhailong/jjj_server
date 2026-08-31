package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.alliance.service.AllianceCacheService;
import com.jjg.game.alliance.service.AllianceHelpService;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEventManager;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.TogetherPlayReconnectDao;
import com.jjg.game.core.service.PlayerStatService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.season.service.SeasonRankingService;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.listener.SimTaskStateReporter;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.NotifyCasinoUpgrade;
import com.jjg.game.sim.pb.res.ResCasinoUpgradeCondition;
import com.jjg.game.sim.pb.res.ResSimCasinoInfo;
import com.jjg.game.sim.pb.res.ResSwitchCasino;
import com.jjg.game.sim.pb.res.ResUnlockCasino;
import com.jjg.game.sim.pb.struct.CasinoUpgradeCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author 11
 * @date 2026/5/28
 */
@Service
public class SimCasinoService implements SimTaskStateReporter {

    private final Logger log = LoggerFactory.getLogger(SimCasinoService.class);

    //新建建筑初始等级
    private static final int INITIAL_BUILDING_LEVEL = 1;

    private final String TABLE_NAME = "simCasinoUnlock";

    @Autowired
    private SimCasinoDao simCasinoDao;
    @Autowired
    private SimBuildingService simBuildingService;
    @Autowired
    private SimConfigCacheService configCacheService;
    @Autowired
    private SimEmployeeService employeeService;
    @Autowired
    private SimAutoSaveService autoSaveService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SimSkillService simSkillService;
    @Autowired
    private SimTaskService simTaskService;
    @Autowired
    private AllianceCacheService allianceCacheService;
    @Autowired
    private AllianceHelpService allianceHelpService;
    @Autowired
    private SimGuestService simGuestService;
    @Autowired
    private SimGuideService simGuideService;
    @Autowired
    private PlayerStatService playerStatService;
    @Autowired
    private SimCoopTaskService simCoopTaskService;
    @Autowired
    private SeasonRankingService seasonRankingService;
    @Autowired
    private GameEventManager gameEventManager;
    @Autowired
    private SimEmployeeRedDotService employeeRedDotService;
    @Autowired
    private TogetherPlayReconnectDao togetherPlayReconnectDao;


    /**
     * 开辟新场景 (校验 condition 后创建并落库, 不自动切换)
     */
    public void onUnlockCasino(SimPlayerContext ctx, int targetCasinoId) {
        ResUnlockCasino res = new ResUnlockCasino(Code.SUCCESS);
        res.casinoId = targetCasinoId;
        try {
            CasinoListCfg cfg = GameDataManager.getCasinoListCfg(targetCasinoId);
            if (cfg == null) {
                log.warn("开辟新场景失败, 配置不存在 playerId={},casinoId={}", ctx.playerId(), targetCasinoId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            //已拥有?
            if (simCasinoDao.findOne(ctx.playerId(), targetCasinoId) != null) {
                log.warn("开辟新场景失败, 已拥有 playerId={},casinoId={}", ctx.playerId(), targetCasinoId);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            //condition: 前置场景经营等级达标
            if (!checkCondition(ctx, cfg)) {
                res.code = Code.NOT_ENOUGH;
                ctx.send(res);
                return;
            }
            SimCasinoData casino = buildNewCasino(ctx, targetCasinoId);
            simCasinoDao.save(casino);
            if (casino.getBuildingData() != null) {
                casino.getBuildingData().values().forEach(building ->
                        simTaskService.onConditionEvent(ctx, new ActionConditionEvent(
                                ActionConditionEvent.Type.BUILDING_LEVEL, building.getId(), 0,
                                building.getLevel(), 1, 0, false)));
            }
            log.info("开辟新场景成功 playerId={},casinoId={}", ctx.playerId(), targetCasinoId);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 切换场景 (落库旧场景, 加载并下发目标场景数据)
     */
    public void onSwitchCasino(SimPlayerContext ctx, int targetCasinoId) {
        ResSwitchCasino res = new ResSwitchCasino(Code.SUCCESS);
        try {
            if (targetCasinoId == ctx.getSimBaseData().getCurrentCasinoId()) {
                log.warn("切换场景失败, 已在该场景 playerId={},casinoId={}", ctx.playerId(), targetCasinoId);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }
            //目标场景必须已拥有
            SimCasinoData target = simCasinoDao.findOne(ctx.playerId(), targetCasinoId);
            if (target == null) {
                log.warn("切换场景失败, 目标场景未拥有 playerId={},casinoId={}", ctx.playerId(), targetCasinoId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            //结算旧场景已积累产出, 再同步落库 (先排空异步在途写, 避免旧快照覆盖)
            SimCasinoData old = ctx.getCurrentCasino();
            if (old != null) {
                simBuildingService.settleOnlineOutput(ctx);
                autoSaveService.awaitPending();
                simCasinoDao.save(old);
            }
            //切换到目标场景 (新加载实体的运行时 transient 字段天然为初始值)
            ctx.setCurrentCasino(target);
            ctx.switchCasino(targetCasinoId);
            simGuestService.updateSpecialGuestRedDots(ctx.playerId());
            employeeRedDotService.updateRedDots(ctx.playerId(),
                    SimConstant.Employee.RED_DOT_GUEST_STAR_UP);
            //先消费联盟助力抵扣(可能使CD提前到时), 再判定完成, 最后下发建筑列表
            simBuildingService.applyPendingSpeedup(ctx, target, System.currentTimeMillis());

            SimCasinoData casino = ctx.getCurrentCasino();
            res.currentCasinoId = casino.getCasinoId();
            res.buildings = SimPbConverter.toBuildingInfos(casino);
            res.managerEmployInfos = SimPbConverter.toManagerInfos(casino);
            res.awareness = casino.getAwareness();

            log.info("切换场景 playerId={},res={}", ctx.playerId(), JSONObject.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 获取场景信息
     *
     * @param ctx
     */
    public void onCasinoInfo(SimPlayerContext ctx) {
        ResSimCasinoInfo res = new ResSimCasinoInfo(Code.SUCCESS);
        try {
            SimCasinoData casinoData = ctx.getCurrentCasino();
            if (casinoData == null) {
                log.warn("获取场景信息失败, 当前场景为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }
            res.level = casinoData.getCasinoLevel();
            res.exp = casinoData.getExp();

            CasinoStatsSheetCfg cfg = configCacheService.getCasinoStatsSheetCfg(casinoData.getCasinoId(), casinoData.getCasinoLevel());
            res.upgradeCost = cfg == null ? 0 : cfg.getUpgradeCost();
            res.allianceId = allianceCacheService.getAllianceId(ctx.playerId());
            res.casinoId = casinoData.getCasinoId();
            AllianceHelpService.SpeedupQuota quota = allianceHelpService.speedupQuota(ctx.playerId());
            res.remainHelp = quota.remainHelp();
            res.dailyHelpLimit = quota.dailyHelpLimit();
            res.remainShare = quota.remainShare();
            res.dailyShareLimit = quota.dailyShareLimit();
            res.coopTaskInfo = simCoopTaskService.getBoundRoomInfo(ctx.playerId());

            //必须是res.coopTaskInfo为空，因为res.coopTaskInfo会拉入到任务房间
            if (res.coopTaskInfo == null) {
                res.togetherPlayGameType = getTogetherPlayReconnectGameType(ctx.playerId());
            }

            //获取下一等级的配置
            CasinoStatsSheetCfg nextLevelCfg = configCacheService.getCasinoStatsSheetCfg(casinoData.getCasinoId(), casinoData.getCasinoLevel() + 1);
            res.upgradeLevelConditions = toUpgradeLevelConditions(nextLevelCfg, ctx);
            long now = System.currentTimeMillis();
            SimBaseData base = ctx.getSimBaseData();
            base.resetOnlineRewardDay(Integer.parseInt(TimeHelper.getDate(now, "yyyyMMdd")));
            res.adCdEndTime = base.getOnlineRewardAdCdEndTime() > now ? base.getOnlineRewardAdCdEndTime() : 0;
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    public void onCasinoUpgradeCondition(SimPlayerContext ctx) {
        ResCasinoUpgradeCondition res = new ResCasinoUpgradeCondition(Code.SUCCESS);
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("获取场景升级条件失败, 当前场景为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
            } else {
                CasinoStatsSheetCfg nextLevelCfg = configCacheService.getCasinoStatsSheetCfg(
                        casino.getCasinoId(), casino.getCasinoLevel() + 1);
                res.conditions = toUpgradeLevelConditions(nextLevelCfg, ctx);
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    private int getTogetherPlayReconnectGameType(long playerId) {
        try {
            return togetherPlayReconnectDao.getGameType(playerId);
        } catch (Exception e) {
            log.warn("查询好友同玩断线恢复标记失败 playerId={}", playerId, e);
            return 0;
        }
    }


    /**
     * 加载场景数据
     *
     * @param ctx
     * @param baseData
     * @return 当前场景; 加载失败返回 null
     */
    public SimCasinoData loadCasinoData(SimPlayerContext ctx, SimBaseData baseData) {
        //预热已解锁场景缓存: 供本玩家高频看板读取, 避免每次访问都同步读 Redis (新玩家为 null, 建默认场景时刷新)
        ctx.setCasinoUnlock(getCasinoUnlock(ctx.playerId()));
        SimCasinoData currentCasino;
        if (baseData.getCurrentCasinoId() > 0) {
            //加载当前所在场景
            currentCasino = simCasinoDao.findOne(ctx.playerId(), baseData.getCurrentCasinoId());
        } else {
            //新玩家: 初始化默认场景并落库
            currentCasino = initDefaultCasino(ctx);
            if (currentCasino != null) {
                baseData.setCurrentCasinoId(currentCasino.getCasinoId());
                simCasinoDao.save(currentCasino);
            }
        }

        if (currentCasino == null) {
            log.warn("加载场景数据失败 playerId={}", ctx.playerId());
            return null;
        }

        ctx.setCurrentCasino(currentCasino);
        //检查是否有建筑完成升级
        simBuildingService.completeAllBuildingUpgrade(ctx, currentCasino);
        return currentCasino;
    }

    /**
     * 初始化新玩家的默认场景 (取 CasinoList 中无解锁条件的场景, 通常为 casinoId=1)
     */
    public SimCasinoData initDefaultCasino(SimPlayerContext ctx) {
        int defaultCasinoId = SimConstant.Common.DEFAULT_CASINO_ID;
        for (Map.Entry<Integer, CasinoListCfg> en : GameDataManager.getCasinoListCfgMap().entrySet()) {
            Map<Integer, Integer> condition = en.getValue().getCondition();
            if (condition == null || condition.isEmpty()) {
                defaultCasinoId = en.getValue().getId();
                break;
            }
        }
        return buildNewCasino(ctx, defaultCasinoId);
    }

    /**
     * 配置驱动创建一个新场景 (初始化/开辟共用): 设置经营等级、繁荣度, 并放入该场景的初始建筑。
     *
     * @param casinoId 场景id (= CasinoListCfg.id = BuildingAreaTableCfg.RegionID)
     */
    public SimCasinoData buildNewCasino(SimPlayerContext ctx, int casinoId) {
        SimCasinoData casino = new SimCasinoData();
        casino.setPlayerId(ctx.playerId());
        casino.setCasinoId(casinoId);

        int statsId = resolveInitialStatsId(casinoId);
        CasinoStatsSheetCfg statsCfg = GameDataManager.getCasinoStatsSheetCfg(statsId);
        if (statsCfg != null) {
            casino.setProsperity(statsCfg.getProsperity());
            casino.setCasinoLevel(statsCfg.getLevel());
        }

        //自动解锁: UnlockType=false 且无解锁条件(UnlockMethod) 的建筑, 创建场景时直接以初始等级解锁
        Set<Integer> unlockedGames = autoUnlockBuildings(casino, casinoId);
        //自动解锁游客
        autoUnlockGuest(casino, casinoId);

        updateCasinoUnlock(ctx, casinoId, unlockedGames);
        simSkillService.initUnlock(ctx, casinoId);
        addAllLevel(ctx, casino.getCasinoLevel());
        simGuideService.triggerSceneTotalLevelReached(ctx, ctx.getSimBaseData().getAllLevel(), true);
        simTaskService.onConditionEvent(ctx,
                SimConditionEventFactory.sceneLevel(casinoId, casino.getCasinoLevel()));
        //TODO 初始游客: VisitorQuest 无场景维度配置, 待策划补充配置后在此初始化 guestMap
        log.info("创建新场景 playerId={},casinoId={},statsId={},buildingCount={}", ctx.playerId(), casinoId, statsId,
                casino.getBuildingData() == null ? 0 : casino.getBuildingData().size());
        return casino;
    }

    /**
     * 增加当前场景经验并连续结算可提升的等级。每次升级校验目标等级配置的建筑条件；amount=0 时仅重新判定。
     * 多级提升只触发一次引导和任务状态更新，避免按等级重复调用。
     */
    public void addCasinoExp(SimPlayerContext ctx, long amount) {
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null || amount < 0) {
            return;
        }

        long exp = casino.getExp() + amount;
        int oldLevel = casino.getCasinoLevel();
        CasinoStatsSheetCfg currentCfg;
        CasinoStatsSheetCfg nextCfg;
        while (true) {
            currentCfg = configCacheService.getCasinoStatsSheetCfg(
                    casino.getCasinoId(), casino.getCasinoLevel());
            nextCfg = configCacheService.getCasinoStatsSheetCfg(
                    casino.getCasinoId(), casino.getCasinoLevel() + 1);
            if (currentCfg == null || nextCfg == null || currentCfg.getUpgradeCost() <= 0
                    || exp < currentCfg.getUpgradeCost()) {
                break;
            }
            Map<Integer, Integer> levelUpCondition = nextCfg.getLevelUpCondition();
            if (levelUpCondition != null && !levelUpCondition.isEmpty()) {
                boolean matched = true;
                for (Map.Entry<Integer, Integer> condition : levelUpCondition.entrySet()) {
                    BuildingData building = casino.findBuilding(condition.getKey());
                    if (building == null || building.getLevel() < condition.getValue()) {
                        matched = false;
                        break;
                    }
                }
                if (!matched) {
                    break;
                }
            }
            exp -= currentCfg.getUpgradeCost();
            casino.setCasinoLevel(nextCfg.getLevel());
        }
        casino.setExp((int) Math.min(exp, Integer.MAX_VALUE));
        int addedLevels = casino.getCasinoLevel() - oldLevel;
        if (addedLevels <= 0) {
            return;
        }
        int oldAllLevel = ctx.getSimBaseData().getAllLevel();
        addAllLevel(ctx, addedLevels);
        notifyCasinoUpgrade(ctx, casino, currentCfg, nextCfg, oldAllLevel, ctx.getSimBaseData().getAllLevel());
        simGuideService.triggerSceneTotalLevelReached(ctx, ctx.getSimBaseData().getAllLevel(), true);
        simTaskService.onConditionEvent(ctx,
                SimConditionEventFactory.sceneLevel(casino.getCasinoId(), casino.getCasinoLevel()));
        log.info("场景升级 playerId={},casinoId={},oldLevel={},newLevel={}",
                casino.getPlayerId(), casino.getCasinoId(), oldLevel, casino.getCasinoLevel());
    }

    /**
     * 修改模拟经营总等级并发布等级变化事件。
     */
    public void addAllLevel(SimPlayerContext ctx, int addedLevels) {
        if (ctx == null || ctx.getSimBaseData() == null || addedLevels == 0) {
            return;
        }
        int oldAllLevel = ctx.getSimBaseData().getAllLevel();
        ctx.getSimBaseData().addAllLevel(addedLevels);
        triggerAllLevelEvent(ctx, oldAllLevel);
    }

    private void triggerAllLevelEvent(SimPlayerContext ctx, int oldAllLevel) {
        if (ctx.getPlayer() != null) {
            gameEventManager.triggerEvent(new PlayerEvent(ctx.getPlayer(), EGameEventType.SIM_ALL_LEVEL,
                    oldAllLevel, ctx.getSimBaseData().getAllLevel()));
        }
    }

    private void notifyCasinoUpgrade(SimPlayerContext ctx, SimCasinoData casino,
                                     CasinoStatsSheetCfg currentCfg, CasinoStatsSheetCfg nextCfg, int oldAllLevel, int newAllLevel) {
        NotifyCasinoUpgrade notify = new NotifyCasinoUpgrade();
        notify.level = casino.getCasinoLevel();
        notify.exp = casino.getExp();
        notify.upgradeCost = currentCfg == null ? 0 : currentCfg.getUpgradeCost();
        notify.upgradeLevelConditions = toUpgradeLevelConditions(nextCfg, ctx);

        //新解锁的建筑
        List<BuildingAreaTableCfg> cfgs = configCacheService.getCasinoLevelBuildingAreaTableCfgs(casino.getCasinoId(), casino.getCasinoLevel());
        if (cfgs != null && !cfgs.isEmpty()) {
            notify.newBuilds = new ArrayList<>();
            for (BuildingAreaTableCfg c : cfgs) {
                notify.newBuilds.add(c.getId());
            }
        }

        //新解锁的功能
        notify.newFunctions = configCacheService.getAllLevelUnlockFunctions(ctx.getSimBaseData().getAllLevel());

        //建筑等级上限提升
        notify.buildLevelMaxInfos = configCacheService.getBuildingLevelMaxInfos(
                casino.getCasinoId(), oldAllLevel, newAllLevel);
        ctx.send(notify);
    }

    private List<CasinoUpgradeCondition> toUpgradeLevelConditions(CasinoStatsSheetCfg nextLevelCfg, SimPlayerContext ctx) {
        if (nextLevelCfg == null || nextLevelCfg.getLevelUpCondition() == null
                || nextLevelCfg.getLevelUpCondition().isEmpty()) {
            return null;
        }
        List<CasinoUpgradeCondition> conditions = new ArrayList<>(nextLevelCfg.getLevelUpCondition().size());
        for (Map.Entry<Integer, Integer> condition : nextLevelCfg.getLevelUpCondition().entrySet()) {
            int buildingId = condition.getKey();
            int needLevel = condition.getValue();
            BuildingData building = ctx.getCurrentCasino().findBuilding(buildingId);

            CasinoUpgradeCondition buc = new CasinoUpgradeCondition();
            buc.buildingId = buildingId;
            if (building == null) {
                buc.unlock = false;
                buc.level = needLevel;
            } else if (building.getLevel() < needLevel) {
                buc.unlock = true;
                buc.level = needLevel;
                buc.canUpgrade = simBuildingService.canUpgradeBuilding(ctx, buildingId);
            } else {
                continue;
            }
            conditions.add(buc);
        }
        return conditions;
    }

    /**
     * 自动解锁场景内无需条件的建筑: UnlockType=false 且 UnlockMethod 为空的建筑,
     * 创建场景时即以初始等级放入 (存在于 buildingData 中即视为已解锁)。
     *
     * @param casinoId 场景id (= BuildingAreaTableCfg.RegionID)
     */
    private Set<Integer> autoUnlockBuildings(SimCasinoData casino, int casinoId) {
        Set<Integer> unlockedGames = new HashSet<>();
        for (BuildingAreaTableCfg cfg : GameDataManager.getBuildingAreaTableCfgList()) {
            if (cfg.getRegionID() != casinoId) {
                continue;
            }
            if (cfg.getUnlockType()) {
                continue;
            }
            if (cfg.getUnlockMethod() != null && !cfg.getUnlockMethod().isEmpty()) {
                continue;
            }
            BuildingData data = new BuildingData();
            data.setId(cfg.getId());
            data.setLevel(INITIAL_BUILDING_LEVEL);
            casino.putBuilding(data);
            if (cfg.getUnlockGameId() > 0) {
                unlockedGames.add(cfg.getUnlockGameId());
            }
        }
        return unlockedGames;
    }

    /**
     * 自动解锁游客
     *
     * @param casinoId 场景id (= BuildingAreaTableCfg.RegionID)
     */
    private void autoUnlockGuest(SimCasinoData casino, int casinoId) {
        List<VisitorQuestCfg> cfgs = configCacheService.getRegionVistorCfgMap().get(casinoId);
        if (cfgs == null || cfgs.isEmpty()) {
            return;
        }
        for (VisitorQuestCfg c : cfgs) {
            if (c.getIsDefaultUnlocked()) {
                simGuestService.unlockGuest(casino, c.getId());
            }
        }
    }

    /**
     * 取某场景 (RegionID) level 最小的经营等级 statsId; 未配置则回退默认值
     */
    private int resolveInitialStatsId(int casinoId) {
        int bestId = SimConstant.Common.DEFAULT_CASINO_STATS_ID;
        int bestLevel = Integer.MAX_VALUE;
        boolean found = false;
        for (CasinoStatsSheetCfg cfg : GameDataManager.getCasinoStatsSheetCfgList()) {
            if (cfg.getRegionID() != casinoId) {
                continue;
            }
            if (cfg.getLevel() < bestLevel) {
                bestLevel = cfg.getLevel();
                bestId = cfg.getId();
                found = true;
            }
        }
        if (!found) {
            log.warn("场景未配置 CasinoStatsSheet, 回退默认 statsId casinoId={},defaultStatsId={}", casinoId, bestId);
        }
        return bestId;
    }

    /**
     * 校验开辟条件: condition 为 (前置场景id -> 需达经营等级); 任一前置场景不达标即失败。
     */
    private boolean checkCondition(SimPlayerContext ctx, CasinoListCfg cfg) {
        Map<Integer, Integer> condition = cfg.getCondition();
        if (condition == null || condition.isEmpty()) {
            return true;
        }
        for (Map.Entry<Integer, Integer> en : condition.entrySet()) {
            int preCasinoId = en.getKey();
            int needLevel = en.getValue();
            int level = casinoOperationLevel(ctx, preCasinoId);
            if (level < needLevel) {
                log.info("开辟新场景条件不满足 playerId={},targetCasinoId={},preCasinoId={},needLevel={},level={}",
                        ctx.playerId(), cfg.getId(), preCasinoId, needLevel, level);
                return false;
            }
        }
        return true;
    }

    /**
     * 取玩家某场景的经营等级 (优先内存当前场景, 否则查 DB); 未拥有返回 -1。
     */
    private int casinoOperationLevel(SimPlayerContext ctx, int casinoId) {
        SimCasinoData casino;
        if (ctx.getCurrentCasino() != null && ctx.getCurrentCasino().getCasinoId() == casinoId) {
            casino = ctx.getCurrentCasino();
        } else {
            casino = simCasinoDao.findOne(ctx.playerId(), casinoId);
        }
        if (casino == null) {
            return -1;
        }
        CasinoStatsSheetCfg statsCfg = configCacheService.getCasinoStatsSheetCfg(casinoId, casino.getCasinoLevel());
        return statsCfg == null ? 0 : statsCfg.getLevel();
    }

    /**
     * 获取知名度
     *
     * @param ctx
     * @return
     */
    public long awareness(SimPlayerContext ctx) {
        SimCasinoData casinoData = ctx.getCurrentCasino();
        if (casinoData == null) {
            return 0;
        }

        if (casinoData.getBuildingData() == null || casinoData.getBuildingData().isEmpty()) {
            return 0;
        }

        //获取运营部的建筑
        BuildingData buildingData = casinoData.getBuildingData().get(SimConstant.Building.ID_OPERATIONS_DEPART);
        if (buildingData == null) {
            return 0;
        }

        //获取配置
        BuildingUpgradeTableCfg cfg = configCacheService.getBuildingUpgradeCfg(buildingData.getId(), buildingData.getLevel());
        if (cfg == null || cfg.getUpgradeOutput() < 1) {
            return 0;
        }

        //获取雇员加成
        Map<BuildingOutputType, Integer> bonusesMap = new HashMap<>();
        employeeService.computeEmployeeLevelBonus(ctx, bonusesMap);
        //运营部产出知名度: 加成 = 管理区集合体(MANAGE_ARRT, 雇员/主管) + 知名度专项(AWARENESS, 勋章)
        int bouns = bonusesMap.getOrDefault(BuildingOutputType.MANAGE_ARRT, 0)
                + bonusesMap.getOrDefault(BuildingOutputType.AWARENESS, 0);
        if (bouns <= 0) {
            return cfg.getUpgradeOutput();
        }
        long extra = cfg.getUpgradeOutput() * bouns / SimConstant.Common.EMPLOYEE_BONUS_DIVISOR;
        return cfg.getUpgradeOutput() + extra;
    }

    /**
     * 更新场景及其建筑解锁的游戏快照。一个场景的自动解锁游戏批量合并后只写一次 Redis。
     *
     * @param ctx      玩家上下文
     * @param casinoId 场景id
     * @param gameIds  本次由建筑解锁的游戏id
     */
    public void updateCasinoUnlock(SimPlayerContext ctx, int casinoId, Collection<Integer> gameIds) {
        SimCasinoUnlock casinoUnlock = ctx.getCasinoUnlock();
        if (casinoUnlock == null) {
            //缓存未命中时回源一次, 避免覆盖 Redis 中已有的解锁记录
            casinoUnlock = getCasinoUnlock(ctx.playerId());
            if (casinoUnlock == null) {
                casinoUnlock = new SimCasinoUnlock();
            }
            ctx.setCasinoUnlock(casinoUnlock);
        }
        Set<Integer> before = casinoUnlock.findUnlockedGameIds();
        boolean changed = casinoUnlock.unlockCasino(casinoId);
        if (gameIds != null) {
            for (Integer gameId : gameIds) {
                if (gameId != null) {
                    changed |= casinoUnlock.unlockGame(casinoId, gameId);
                }
            }
        }
        if (!changed) {
            return;
        }
        redisTemplate.opsForHash().put(TABLE_NAME, ctx.playerId(), casinoUnlock);
        int newGameCount = casinoUnlock.findUnlockedGameIds().size() - before.size();
        if (newGameCount > 0) {
            playerStatService.recordGameUnlock(ctx.playerId(), newGameCount);
            reportUnlockedGames(ctx);
        }
    }

    /**
     * 上报当前已解锁游戏数 (任务条件 12216)。
     * <p>
     * 上报总数而非增量, 条件按 SET 覆盖进度, 重复上报幂等; 登录期场景数据先于任务数据加载, 那时的上报会被
     * 任务侧忽略, 故 SimTaskService 在任务数据就绪后作为 {@link SimTaskStateReporter} 补报一次。
     * 这里直接用 ctx 投递, 不走 AllianceEventService 的 registry 查找 (登录期 ctx 尚未入 registry)。
     */
    @Override
    public void reportTaskState(SimPlayerContext ctx, Consumer<ActionConditionEvent> sink) {
        reportUnlockedGames(ctx, sink);
    }

    public void reportUnlockedGames(SimPlayerContext ctx) {
        reportUnlockedGames(ctx, e -> simTaskService.onConditionEvent(ctx, e));
    }

    private void reportUnlockedGames(SimPlayerContext ctx, Consumer<ActionConditionEvent> sink) {
        SimCasinoUnlock casinoUnlock = ctx.getCasinoUnlock();
        if (casinoUnlock == null) {
            return;
        }
        int unlocked = casinoUnlock.findUnlockedGameIds().size();
        if (unlocked > 0) {
            sink.accept(SimConditionEventFactory.gameUnlocked(unlocked));
        }
    }

    public SimCasinoUnlock getCasinoUnlock(long playerId) {
        return (SimCasinoUnlock) redisTemplate.opsForHash().get(TABLE_NAME, playerId);
    }
}
