package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.VisitorQuestCfg;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.BuildingType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.pb.res.ResOperationCapacity;
import com.jjg.game.sim.pb.res.ResOperationDashboard;
import com.jjg.game.sim.pb.struct.BuildingTips;
import com.jjg.game.sim.pb.struct.OperationBuildingCapacity;
import com.jjg.game.sim.pb.struct.OperationBuildingData;
import com.jjg.game.sim.pb.struct.OperationDashboardOverview;
import com.jjg.game.sim.pb.struct.OperationResearchBuilding;
import com.jjg.game.sim.pb.struct.OperationVisitorQualityRate;
import com.jjg.game.sim.tools.SimTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新版细分运营数据看板。
 * <p>
 * 本服务使用独立消息和数据结构，不复用也不修改旧版 {@link SimStatsService} 的累计总数据口径。
 */
@Service
public class SimOperationDashboardService implements IRedDotService, SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SimOperationDashboardService.class);
    private static final DateTimeFormatter SATISFACTION_LOG_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    //测试核对用，只在请求完整看板时输出；不需要排查时可通过配置关闭。
    @Value("${sim.dashboard.satisfaction-log-enabled:true}")
    private boolean satisfactionLogEnabled = true;
    //游客生成由2秒总tick驱动；入口告警只需低频刷新，默认30秒检查一次。
    @Value("${sim.dashboard.warning-check-interval-ms:30000}")
    private long warningCheckIntervalMs = 30_000L;

    private static final int RESEARCH_UNLOCKED = 1;
    private static final int RESEARCH_CURRENT = 2;
    private static final int RESEARCH_FUTURE = 3;

    //策划要求只展示蓝、紫、橙三档高级游客，对应 VisitorQuest.Quality 3/4/5。
    private static final List<Integer> PREMIUM_QUALITIES = List.of(3, 4, 5);

    @Autowired
    private SimBuildingService buildingService;
    @Autowired
    private SimGuestService guestService;
    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private SimPlayerContextRegistry contextRegistry;
    @Autowired
    private RedDotManager redDotManager;

    /**
     * 获取进入数据页时使用的完整看板数据。
     */
    public void onDashboard(SimPlayerContext ctx) {
        ResOperationDashboard res = new ResOperationDashboard(Code.SUCCESS);
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("获取细分运营看板失败, 当前场景为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
            } else {
                long now = System.currentTimeMillis();
                res.casinoId = casino.getCasinoId();
                res.overview = buildOverview(ctx, casino, now, true);
                res.buildings = buildBuildingData(ctx, casino, res.overview, now);
                res.researchBuildings = buildResearchBuildings(ctx, casino);
                log.info("返回细分运营看板 playerId={},casinoId={},buildingCount={},researchCount={}",
                        ctx.playerId(), res.casinoId, res.buildings.size(), res.researchBuildings.size());
            }
        } catch (Exception e) {
            log.error("获取细分运营看板异常 playerId={}", ctx.playerId(), e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 获取停留在数据页时每秒刷新的轻量容纳数据。
     */
    public void onCapacity(SimPlayerContext ctx) {
        ResOperationCapacity res = new ResOperationCapacity(Code.SUCCESS);
        try {
            SimCasinoData casino = ctx.getCurrentCasino();
            if (casino == null) {
                log.warn("获取看板实时容纳数据失败, 当前场景为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
            } else {
                long now = System.currentTimeMillis();
                res.casinoId = casino.getCasinoId();
                res.buildings = buildCapacityData(casino, now);
                res.totalCapacity = res.buildings.stream().mapToInt(data -> data.capacity).sum();
                int currentCapacity = casino.countGenerateInWindow(now, SimConstant.Common.CAPACITY_WINDOW_MS);
                res.currentCapacity = Math.min(res.totalCapacity, currentCapacity);
                res.capacityOverloaded = isCapacityOverloaded(currentCapacity, res.totalCapacity);
            }
        } catch (Exception e) {
            log.error("获取看板实时容纳数据异常 playerId={}", ctx.playerId(), e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    private OperationDashboardOverview buildOverview(SimPlayerContext ctx, SimCasinoData casino, long now,
                                                     boolean writeSatisfactionLog) {
        OperationDashboardOverview overview = new OperationDashboardOverview();
        Map<BuildingOutputType, Long> outputs = buildingService.computePerMinuteOutput(ctx, casino);
        overview.goldOutputPerMinute = outputs.getOrDefault(BuildingOutputType.GOLD, 0L) * 60;
        overview.expOutputPerMinute = outputs.getOrDefault(BuildingOutputType.CASINO_LEVEL_EXP, 0L) * 60;
        overview.totalCapacity = computeCurrentCapacity(casino);
        //窗口累计人数仅用于展示，按当前容纳上限截断，不修改原始游客/交互统计。
        int currentCapacity = casino.countGenerateInWindow(now, SimConstant.Common.CAPACITY_WINDOW_MS);
        overview.currentCapacity = Math.min(overview.totalCapacity, currentCapacity);
        overview.capacityOverloaded = isCapacityOverloaded(currentCapacity, overview.totalCapacity);
        overview.serviceCapacity = buildingService.computeDeptValue(ctx, casino, BuildingOutputType.SERVICE);
        overview.awareness = buildingService.computeDeptValue(ctx, casino, BuildingOutputType.AWARENESS);
        long exposure = buildingService.computeDeptValue(ctx, casino, BuildingOutputType.EXPOSURE);

        CasinoStatsSheetCfg casinoCfg = configCache.getCasinoStatsSheetCfg(
                casino.getCasinoId(), casino.getCasinoLevel());
        overview.customerAcquisitionPerMinute = customerAcquisitionPerMinute(casinoCfg, casino, now) * 60;
        overview.operationRate = operationRate(exposure, casinoCfg);
        overview.totalProsperity = buildingService.computeProsperity(casino);
        overview.standardInteractionCount = buildingService.computeStandardInteractionCount(casino);
        overview.satisfactionRate = satisfactionRate(ctx.playerId(), casino,
                overview.standardInteractionCount, now, writeSatisfactionLog);
        overview.premiumVisitorRates = premiumVisitorRates(casino);
        overview.receptionIncomeTooLow = overview.serviceCapacity > 0 && isRateBelow(overview.satisfactionRate,
                SimConstant.Dashboard.GLOBAL_SATISFACTION_LOW_RATE_ID,
                SimConstant.Dashboard.DEFAULT_SATISFACTION_LOW_RATE);
        overview.operationIncomeTooLow = exposure > 0 && isRateBelow(overview.operationRate,
                SimConstant.Dashboard.GLOBAL_ACQUISITION_LOW_RATE_ID,
                SimConstant.Dashboard.DEFAULT_ACQUISITION_LOW_RATE);
        overview.hasWarning = overview.receptionIncomeTooLow || overview.operationIncomeTooLow
                || hasGameCapacityWarning(casino, now);
        overview.promptLanguageId = promptLanguageId(overview);
        return overview;
    }

    private List<OperationBuildingData> buildBuildingData(SimPlayerContext ctx, SimCasinoData casino,
                                                          OperationDashboardOverview overview, long now) {
        Map<Integer, Integer> expectedLevels = expectedBuildingLevels(casino);
        List<OperationBuildingData> result = new ArrayList<>();
        if (casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return result;
        }

        for (BuildingAreaTableCfg areaCfg : GameDataManager.getBuildingAreaTableCfgList()) {
            if (areaCfg.getRegionID() != casino.getCasinoId()) {
                continue;
            }
            BuildingType buildingType = BuildingType.fromCode(areaCfg.getType());
            if (buildingType != BuildingType.MANAGE
                    && buildingType != BuildingType.REST
                    && buildingType != BuildingType.GAME) {
                continue;
            }
            BuildingData building = casino.findBuilding(areaCfg.getId());
            if (building == null) {
                continue;
            }

            OperationBuildingData data = new OperationBuildingData();
            data.buildingId = building.getId();
            data.buildingType = areaCfg.getType();
            data.level = building.getLevel();
            data.maxLevel = areaCfg.getMaxLevel();
            data.expectedLevel = expectedLevels.getOrDefault(building.getId(), 0);

            BuildingUpgradeTableCfg levelCfg = configCache.getBuildingUpgradeCfg(building.getId(), building.getLevel());
            int currentCapacity = 0;
            if (levelCfg != null && (buildingType == BuildingType.GAME || buildingType == BuildingType.REST)) {
                data.capacity = levelCfg.getMaxInteractionCount();
                currentCapacity = casino.countBuildingInteractionsInWindow(
                        building.getId(), now, SimConstant.Common.CAPACITY_WINDOW_MS);
                data.currentCapacity = Math.min(data.capacity, currentCapacity);
                data.capacityOverloaded = isCapacityOverloaded(currentCapacity, data.capacity);
            }

            Map<BuildingOutputType, Long> values = buildingService.computeDashboardBuildingValues(ctx, building);
            // 配置及建筑结算返回的是每分钟产出；数据看板前端统一按“/h”展示。
            data.goldOutputPerMinute = values.getOrDefault(BuildingOutputType.GOLD, 0L) * 60;
            data.expOutputPerMinute = values.getOrDefault(BuildingOutputType.CASINO_LEVEL_EXP, 0L) * 60;
            data.powerOutputPerMinute = values.getOrDefault(BuildingOutputType.POWER, 0L) * 60;
            data.serviceCapacity = values.getOrDefault(BuildingOutputType.SERVICE, 0L);
            data.exposure = values.getOrDefault(BuildingOutputType.EXPOSURE, 0L);
            data.awareness = values.getOrDefault(BuildingOutputType.AWARENESS, 0L);

            if (data.serviceCapacity > 0) {
                data.satisfactionRate = overview.satisfactionRate;
            }
            if (data.exposure > 0) {
                data.operationRate = overview.operationRate;
            }
            if (data.awareness > 0) {
                data.premiumVisitorRates = overview.premiumVisitorRates;
            }
            if (data.serviceCapacity > 0) {
                data.incomeTooLow = overview.receptionIncomeTooLow;
                data.warningLanguageId = data.incomeTooLow
                        ? SimConstant.Dashboard.LANG_LABEL_RECEPTION_LOW : 0;
            } else if (data.exposure > 0) {
                data.incomeTooLow = overview.operationIncomeTooLow;
                data.warningLanguageId = data.incomeTooLow
                        ? SimConstant.Dashboard.LANG_LABEL_OPERATION_LOW : 0;
            } else if (buildingType == BuildingType.GAME) {
                data.incomeTooLow = isGameCapacityTooLow(currentCapacity, data.capacity);
                data.warningLanguageId = data.incomeTooLow
                        ? SimConstant.Dashboard.LANG_LABEL_GAME_CAPACITY_LOW : 0;
            }
            result.add(data);
        }

        //收益过低项整体前置；同一状态内再按原有建筑分组和配置解锁顺序排列。
        result.sort(Comparator.<OperationBuildingData, Boolean>comparing(data -> !data.incomeTooLow)
                .thenComparingInt(data -> buildingSortGroup(data.buildingType)));
        return result;
    }

    private List<OperationResearchBuilding> buildResearchBuildings(SimPlayerContext ctx, SimCasinoData casino) {
        List<OperationResearchBuilding> result = new ArrayList<>();
        boolean currentLockedFound = false;
        // 与客户端展示顺序保持一致：经营等级低的游戏优先，同等级按建筑ID排序。
        // 配置容器底层为ConcurrentHashMap，不能依赖getCfgBeanList的遍历顺序。
        List<BuildingAreaTableCfg> researchConfigs = GameDataManager.getBuildingAreaTableCfgList().stream()
                .filter(cfg -> cfg.getRegionID() == casino.getCasinoId()
                        && BuildingType.fromCode(cfg.getType()) == BuildingType.GAME)
                .sorted(Comparator.comparingInt(BuildingAreaTableCfg::getCasinoLevel)
                        .thenComparingInt(BuildingAreaTableCfg::getId))
                .toList();
        for (BuildingAreaTableCfg cfg : researchConfigs) {
            OperationResearchBuilding data = new OperationResearchBuilding();
            data.buildingId = cfg.getId();
            data.gameType = cfg.getUnlockGameId();
            if (casino.findBuilding(cfg.getId()) != null) {
                data.state = RESEARCH_UNLOCKED;
                data.canResearch = false;
            } else if (!currentLockedFound) {
                currentLockedFound = true;
                data.state = RESEARCH_CURRENT;
                data.unmetConditions = buildUnlockConditions(ctx, casino, cfg);
                data.canResearch = data.unmetConditions.isEmpty();
            } else {
                data.state = RESEARCH_FUTURE;
                data.canResearch = false;
                data.unmetConditions = Collections.emptyList();
            }
            result.add(data);
        }
        return result;
    }

    private List<BuildingTips> buildUnlockConditions(SimPlayerContext ctx, SimCasinoData casino,
                                                     BuildingAreaTableCfg cfg) {
        List<BuildingTips> tips = new ArrayList<>();
        if (cfg.getCasinoLevel() > 0 && ctx.getSimBaseData().getAllLevel() < cfg.getCasinoLevel()) {
            addTip(tips, languageId(cfg, 0), String.valueOf(cfg.getCasinoLevel()));
        }

        if (cfg.getUnlockMethod() != null) {
            int languageId = languageId(cfg, 1);
            for (Map.Entry<Integer, Integer> condition : cfg.getUnlockMethod().entrySet()) {
                BuildingData required = casino.findBuilding(condition.getKey());
                if (required != null && required.getLevel() >= condition.getValue()) {
                    continue;
                }
                BuildingAreaTableCfg requiredCfg = GameDataManager.getBuildingAreaTableCfg(condition.getKey());
                if (languageId > 0 && requiredCfg != null) {
                    tips.add(SimTool.buildTips(languageId, requiredCfg.getBuildingNameId(),
                            String.valueOf(condition.getValue())));
                }
            }
        }

        if (cfg.getUnlockCost() != null && !cfg.getUnlockCost().isEmpty()
                && !playerPackService.checkHasItems(ctx.getPlayer(), cfg.getUnlockCost())) {
            int languageId = languageId(cfg, 2);
            if (languageId > 0) {
                Long gold = cfg.getUnlockCost().get(ItemUtils.getGoldItemId());
                tips.add(SimTool.buildTips(languageId, String.valueOf(gold == null ? 0 : gold)));
            }
        }
        return tips;
    }

    private List<OperationBuildingCapacity> buildCapacityData(SimCasinoData casino, long now) {
        List<OperationBuildingCapacity> result = new ArrayList<>();
        if (casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return result;
        }
        for (BuildingAreaTableCfg areaCfg : GameDataManager.getBuildingAreaTableCfgList()) {
            if (areaCfg.getRegionID() != casino.getCasinoId()) {
                continue;
            }
            BuildingType type = BuildingType.fromCode(areaCfg.getType());
            if (type != BuildingType.GAME && type != BuildingType.REST) {
                continue;
            }
            BuildingData building = casino.findBuilding(areaCfg.getId());
            if (building == null) {
                continue;
            }
            BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(building.getId(), building.getLevel());
            if (cfg == null) {
                continue;
            }
            OperationBuildingCapacity data = new OperationBuildingCapacity();
            data.buildingId = building.getId();
            data.capacity = cfg.getMaxInteractionCount();
            int currentCapacity = casino.countBuildingInteractionsInWindow(
                    building.getId(), now, SimConstant.Common.CAPACITY_WINDOW_MS);
            data.currentCapacity = Math.min(data.capacity, currentCapacity);
            data.capacityOverloaded = isCapacityOverloaded(currentCapacity, data.capacity);
            result.add(data);
        }
        return result;
    }

    private int computeCurrentCapacity(SimCasinoData casino) {
        int total = 0;
        if (casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return total;
        }
        for (BuildingData building : casino.getBuildingData().values()) {
            BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(building.getId());
            if (areaCfg == null) {
                continue;
            }
            BuildingType type = BuildingType.fromCode(areaCfg.getType());
            if (type != BuildingType.GAME && type != BuildingType.REST) {
                continue;
            }
            BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(building.getId(), building.getLevel());
            if (cfg != null) {
                total += cfg.getMaxInteractionCount();
            }
        }
        return total;
    }

    public long customerAcquisitionPerMinute(CasinoStatsSheetCfg cfg, SimCasinoData casino, long now) {
        long intervalMs = guestService.computeVisitIntervalMs(cfg, casino, now);
        if (intervalMs <= 0) {
            return 0;
        }
        return 60_000L * cfg.getVisitorSpawnCount() / intervalMs;
    }

    private int operationRate(long exposure, CasinoStatsSheetCfg cfg) {
        if (exposure <= 0 || cfg == null || cfg.getExposureRequirements() <= 0) {
            return 0;
        }
        long rate = exposure * SimConstant.Common.DASHBOARD_RATE_BASE / cfg.getExposureRequirements();
        return (int) Math.min(SimConstant.Common.DASHBOARD_RATE_BASE, rate);
    }

    private int satisfactionRate(long playerId, SimCasinoData casino, int standardInteractionCount,
                                 long now, boolean writeLog) {
        int guestCount = casino.countGenerateInWindow(now, SimConstant.Common.CAPACITY_WINDOW_MS);
        long interactions = casino.countInteractionsInWindow(now, SimConstant.Common.CAPACITY_WINDOW_MS);
        //标准交互次数 = 已解锁建筑当前等级 InteractCount 之和 / 100。
        long denominator = (long) guestCount * standardInteractionCount;
        long rate = denominator <= 0 ? SimConstant.Common.DASHBOARD_RATE_BASE
                : interactions * 100L * SimConstant.Common.DASHBOARD_RATE_BASE / denominator;
        int result = (int) Math.min(SimConstant.Common.DASHBOARD_RATE_BASE, rate);
        if (writeLog && satisfactionLogEnabled && log.isInfoEnabled()) {
            //直接记录本次计算快照，不重复统计；人数不使用前端容纳展示的截断值。
            log.info("看板满意度计算：玩家编号：{}，场景编号：{}，统计时长：最近{}分钟，窗口开始：{}，窗口结束：{}，"
                            + "游客人数（未截断）：{}，规划交互次数：{}，标准交互次数（百分之一）：{}，"
                            + "最终满意度：{}%，计算说明：{}",
                    playerId, casino.getCasinoId(), SimConstant.Common.CAPACITY_WINDOW_MS / 60_000,
                    SATISFACTION_LOG_TIME.format(Instant.ofEpochMilli(now - SimConstant.Common.CAPACITY_WINDOW_MS)),
                    SATISFACTION_LOG_TIME.format(Instant.ofEpochMilli(now)),
                    guestCount, interactions, standardInteractionCount,
                    BigDecimal.valueOf(result, 2).toPlainString(),
                    denominator <= 0 ? "窗口内没有游客或建筑未配置标准交互次数，默认满满意度"
                            : "规划交互次数乘一百，除以游客人数与标准交互次数配置和，最高百分之百");
        }
        return result;
    }

    private boolean hasGameCapacityWarning(SimCasinoData casino, long now) {
        if (casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return false;
        }
        for (BuildingData building : casino.getBuildingData().values()) {
            BuildingAreaTableCfg areaCfg = GameDataManager.getBuildingAreaTableCfg(building.getId());
            if (areaCfg == null || BuildingType.fromCode(areaCfg.getType()) != BuildingType.GAME) {
                continue;
            }
            BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(building.getId(), building.getLevel());
            int capacity = cfg == null ? 0 : cfg.getMaxInteractionCount();
            int current = casino.countBuildingInteractionsInWindow(
                    building.getId(), now, SimConstant.Common.CAPACITY_WINDOW_MS);
            if (isGameCapacityTooLow(current, capacity)) {
                return true;
            }
        }
        return false;
    }

    private boolean isGameCapacityTooLow(int currentCapacity, int standardCapacity) {
        if (standardCapacity <= 0) {
            return false;
        }
        int threshold = globalPercent(SimConstant.Dashboard.GLOBAL_GAME_CAPACITY_LOW_RATE_ID,
                SimConstant.Dashboard.DEFAULT_GAME_CAPACITY_LOW_RATE);
        return (long) currentCapacity * 100 < (long) standardCapacity * threshold;
    }

    private boolean isRateBelow(int rate, int globalId, int defaultPercent) {
        return rate < globalPercent(globalId, defaultPercent) * 100;
    }

    private boolean isCapacityOverloaded(int currentCapacity, int totalCapacity) {
        if (totalCapacity <= 0) {
            return false;
        }
        int threshold = globalPercent(SimConstant.Dashboard.GLOBAL_CAPACITY_OVERLOAD_RATE_ID,
                SimConstant.Dashboard.DEFAULT_CAPACITY_OVERLOAD_RATE);
        return (long) currentCapacity * 100 > (long) totalCapacity * threshold;
    }

    private int globalPercent(int id, int defaultValue) {
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(id);
        return cfg == null || cfg.getIntValue() <= 0 ? defaultValue : cfg.getIntValue();
    }

    private static int promptLanguageId(OperationDashboardOverview overview) {
        if (overview.receptionIncomeTooLow && overview.operationIncomeTooLow) {
            return SimConstant.Dashboard.LANG_PROMPT_RECEPTION_AND_OPERATION_LOW;
        }
        if (overview.receptionIncomeTooLow) {
            return SimConstant.Dashboard.LANG_PROMPT_RECEPTION_LOW;
        }
        if (overview.operationIncomeTooLow) {
            return SimConstant.Dashboard.LANG_PROMPT_OPERATION_LOW;
        }
        return overview.hasWarning ? 0 : SimConstant.Dashboard.LANG_PROMPT_NORMAL;
    }

    /**
     * 定时入口提示只计算告警所需指标，避免每30秒重复计算产出和游客品质概率。
     */
    private boolean hasDashboardWarning(SimPlayerContext ctx, SimCasinoData casino, long now) {
        int standardInteractionCount = buildingService.computeStandardInteractionCount(casino);
        int satisfactionRate = satisfactionRate(ctx.playerId(), casino, standardInteractionCount, now, false);
        long serviceCapacity = buildingService.computeDeptValue(ctx, casino, BuildingOutputType.SERVICE);
        if (serviceCapacity > 0 && isRateBelow(satisfactionRate,
                SimConstant.Dashboard.GLOBAL_SATISFACTION_LOW_RATE_ID,
                SimConstant.Dashboard.DEFAULT_SATISFACTION_LOW_RATE)) {
            return true;
        }

        long exposure = buildingService.computeDeptValue(ctx, casino, BuildingOutputType.EXPOSURE);
        CasinoStatsSheetCfg casinoCfg = configCache.getCasinoStatsSheetCfg(
                casino.getCasinoId(), casino.getCasinoLevel());
        if (exposure > 0 && isRateBelow(operationRate(exposure, casinoCfg),
                SimConstant.Dashboard.GLOBAL_ACQUISITION_LOW_RATE_ID,
                SimConstant.Dashboard.DEFAULT_ACQUISITION_LOW_RATE)) {
            return true;
        }
        return hasGameCapacityWarning(casino, now);
    }

    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.BUILDING;
    }

    @Override
    public List<Integer> getSubmodules() {
        return List.of(SimConstant.Dashboard.RED_DOT_WARNING);
    }

    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        if (submodule != 0 && submodule != SimConstant.Dashboard.RED_DOT_WARNING) {
            return List.of();
        }
        SimPlayerContext ctx = contextRegistry.getContext(playerId);
        if (ctx == null || ctx.getCurrentCasino() == null) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        boolean warning = hasDashboardWarning(ctx, ctx.getCurrentCasino(), now);
        ctx.setDashboardWarningSnapshot(warning);
        ctx.setDashboardWarningCheckTime(now);
        return List.of(buildWarningRedDot(warning));
    }

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        if (ctx == null || ctx.getCurrentCasino() == null || !ctx.getInCasino().get()
                || ctx.getDashboardWarningCheckTime() > 0
                && now - ctx.getDashboardWarningCheckTime() < Math.max(5_000L, warningCheckIntervalMs)) {
            return;
        }
        try {
            ctx.setDashboardWarningCheckTime(now);
            boolean warning = hasDashboardWarning(ctx, ctx.getCurrentCasino(), now);
            Boolean previous = ctx.getDashboardWarningSnapshot();
            ctx.setDashboardWarningSnapshot(warning);
            if (previous == null || previous != warning) {
                redDotManager.updateRedDot(List.of(buildWarningRedDot(warning)), ctx.playerId());
                log.info("刷新数据看板入口感叹号 playerId={},casinoId={},show={}",
                        ctx.playerId(), ctx.getCurrentCasino().getCasinoId(), warning);
            }
        } catch (Exception e) {
            log.error("刷新数据看板入口感叹号异常 playerId={}", ctx.playerId(), e);
        }
    }

    @Override
    public int order() {
        return 120;
    }

    private RedDotDetails buildWarningRedDot(boolean warning) {
        return redDotManager.buildRedDotDetails(getModule(), SimConstant.Dashboard.RED_DOT_WARNING,
                warning ? 1 : 0, RedDotDetails.RedDotType.EXCLAMATION);
    }

    private List<OperationVisitorQualityRate> premiumVisitorRates(SimCasinoData casino) {
        Map<Integer, Long> weights = new HashMap<>();
        long totalWeight = 0;
        if (casino.getGuestMap() != null) {
            for (GuestData guest : casino.getGuestMap().values()) {
                VisitorQuestCfg cfg = guest == null ? null : GameDataManager.getVisitorQuestCfg(guest.getId());
                if (cfg == null) {
                    continue;
                }
                long weight = guestService.effectiveRefreshWeight(casino, cfg);
                if (weight <= 0) {
                    continue;
                }
                weights.merge(cfg.getQuality(), weight, Long::sum);
                totalWeight += weight;
            }
        }

        List<OperationVisitorQualityRate> result = new ArrayList<>(PREMIUM_QUALITIES.size());
        for (Integer quality : PREMIUM_QUALITIES) {
            long weight = weights.getOrDefault(quality, 0L);
            OperationVisitorQualityRate data = new OperationVisitorQualityRate();
            data.quality = quality;
            data.weight = weight;
            data.rate = totalWeight <= 0 ? 0
                    : (int) Math.min(SimConstant.Common.DASHBOARD_RATE_BASE,
                    weight * SimConstant.Common.DASHBOARD_RATE_BASE / totalWeight);
            result.add(data);
        }
        return result;
    }

    private Map<Integer, Integer> expectedBuildingLevels(SimCasinoData casino) {
        CasinoStatsSheetCfg cfg = configCache.getCasinoStatsSheetCfg(
                casino.getCasinoId(), casino.getCasinoLevel());
        if (cfg == null || cfg.getExpectedBuildingLevel() == null) {
            return Collections.emptyMap();
        }
        return cfg.getExpectedBuildingLevel();
    }

    private static int buildingSortGroup(int buildingType) {
        BuildingType type = BuildingType.fromCode(buildingType);
        if (type == BuildingType.MANAGE) {
            return 1;
        }
        if (type == BuildingType.REST) {
            return 2;
        }
        return 3;
    }

    private static int languageId(BuildingAreaTableCfg cfg, int index) {
        List<Integer> ids = cfg.getLanguageID();
        return ids == null || index < 0 || index >= ids.size() ? 0 : ids.get(index);
    }

    private static void addTip(List<BuildingTips> tips, int languageId, String... args) {
        if (languageId > 0) {
            tips.add(SimTool.buildTips(languageId, args));
        }
    }
}
