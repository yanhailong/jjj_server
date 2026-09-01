package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BuildingAreaTableCfg;
import com.jjg.game.sampledata.bean.BuildingUpgradeTableCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sampledata.bean.VisitorQuestCfg;
import com.jjg.game.sim.constant.BuildingOutputType;
import com.jjg.game.sim.constant.BuildingType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimCasinoData;
import com.jjg.game.sim.data.SimPlayerContext;
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
public class SimOperationDashboardService {
    private static final Logger log = LoggerFactory.getLogger(SimOperationDashboardService.class);
    private static final DateTimeFormatter SATISFACTION_LOG_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    //测试核对用，只在请求完整看板时输出；不需要排查时可通过配置关闭。
    @Value("${sim.dashboard.satisfaction-log-enabled:true}")
    private boolean satisfactionLogEnabled = true;

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
                res.overview = buildOverview(ctx, casino, now);
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
                res.currentCapacity = Math.min(res.totalCapacity,
                        casino.countGenerateInWindow(now, SimConstant.Common.CAPACITY_WINDOW_MS));
            }
        } catch (Exception e) {
            log.error("获取看板实时容纳数据异常 playerId={}", ctx.playerId(), e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    private OperationDashboardOverview buildOverview(SimPlayerContext ctx, SimCasinoData casino, long now) {
        OperationDashboardOverview overview = new OperationDashboardOverview();
        Map<BuildingOutputType, Long> outputs = buildingService.computePerMinuteOutput(ctx, casino);
        overview.goldOutputPerMinute = outputs.getOrDefault(BuildingOutputType.GOLD, 0L);
        overview.expOutputPerMinute = outputs.getOrDefault(BuildingOutputType.CASINO_LEVEL_EXP, 0L);
        overview.totalCapacity = computeCurrentCapacity(casino);
        //窗口累计人数仅用于展示，按当前容纳上限截断，不修改原始游客/交互统计。
        overview.currentCapacity = Math.min(overview.totalCapacity,
                casino.countGenerateInWindow(now, SimConstant.Common.CAPACITY_WINDOW_MS));
        overview.serviceCapacity = buildingService.computeDeptValue(ctx, casino, BuildingOutputType.SERVICE);
        overview.awareness = buildingService.computeDeptValue(ctx, casino, BuildingOutputType.AWARENESS);
        long exposure = buildingService.computeDeptValue(ctx, casino, BuildingOutputType.EXPOSURE);

        CasinoStatsSheetCfg casinoCfg = configCache.getCasinoStatsSheetCfg(
                casino.getCasinoId(), casino.getCasinoLevel());
        overview.customerAcquisitionPerMinute = customerAcquisitionPerMinute(exposure, casinoCfg);
        overview.operationRate = operationRate(exposure, casinoCfg);
        overview.satisfactionRate = satisfactionRate(ctx.playerId(), casino, casinoCfg, now);
        overview.premiumVisitorRates = premiumVisitorRates(casino);
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
            data.incomeTooLow = data.expectedLevel > 0 && data.level < data.expectedLevel;

            BuildingUpgradeTableCfg levelCfg = configCache.getBuildingUpgradeCfg(building.getId(), building.getLevel());
            if (levelCfg != null && (buildingType == BuildingType.GAME || buildingType == BuildingType.REST)) {
                data.capacity = levelCfg.getMaxInteractionCount();
                data.currentCapacity = Math.min(data.capacity, casino.countBuildingInteractionsInWindow(
                        building.getId(), now, SimConstant.Common.CAPACITY_WINDOW_MS));
            }

            Map<BuildingOutputType, Long> values = buildingService.computeDashboardBuildingValues(ctx, building);
            data.goldOutputPerMinute = values.getOrDefault(BuildingOutputType.GOLD, 0L);
            data.expOutputPerMinute = values.getOrDefault(BuildingOutputType.CASINO_LEVEL_EXP, 0L);
            data.powerOutputPerMinute = values.getOrDefault(BuildingOutputType.POWER, 0L);
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
            result.add(data);
        }

        //配置顺序决定同类建筑解锁顺序；稳定排序只调整管理区、休息区、Slot区三个分组。
        result.sort(Comparator.comparingInt(data -> buildingSortGroup(data.buildingType)));
        return result;
    }

    private List<OperationResearchBuilding> buildResearchBuildings(SimPlayerContext ctx, SimCasinoData casino) {
        List<OperationResearchBuilding> result = new ArrayList<>();
        boolean currentLockedFound = false;
        for (BuildingAreaTableCfg cfg : GameDataManager.getBuildingAreaTableCfgList()) {
            if (cfg.getRegionID() != casino.getCasinoId()
                    || BuildingType.fromCode(cfg.getType()) != BuildingType.GAME) {
                continue;
            }
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
            data.currentCapacity = Math.min(data.capacity, casino.countBuildingInteractionsInWindow(
                    building.getId(), now, SimConstant.Common.CAPACITY_WINDOW_MS));
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

    private long customerAcquisitionPerMinute(long exposure, CasinoStatsSheetCfg cfg) {
        if (exposure <= 0 || cfg == null || cfg.getExposureRequirements() <= 0
                || cfg.getBaseVisitInterval() <= 0 || cfg.getVisitorSpawnCount() <= 0) {
            return 0;
        }
        return exposure * cfg.getVisitorSpawnCount() * 60L
                / cfg.getExposureRequirements() / cfg.getBaseVisitInterval();
    }

    private int operationRate(long exposure, CasinoStatsSheetCfg cfg) {
        if (exposure <= 0 || cfg == null || cfg.getExposureRequirements() <= 0) {
            return 0;
        }
        long rate = exposure * SimConstant.Common.DASHBOARD_RATE_BASE / cfg.getExposureRequirements();
        return (int) Math.min(SimConstant.Common.DASHBOARD_RATE_BASE, rate);
    }

    private int satisfactionRate(long playerId, SimCasinoData casino, CasinoStatsSheetCfg cfg, long now) {
        int guestCount = casino.countGenerateInWindow(now, SimConstant.Common.CAPACITY_WINDOW_MS);
        int configuredInteractions = cfg == null ? 0 : cfg.getInteractCount();
        int requiredInteractions = configuredInteractions;
        //兼容策划表尚未增加 InteractCount 的环境：按每名游客至少完成一次交互计算。
        if (requiredInteractions <= 0) {
            requiredInteractions = 1;
        }
        long interactions = casino.countInteractionsInWindow(now, SimConstant.Common.CAPACITY_WINDOW_MS);
        long denominator = (long) guestCount * requiredInteractions;
        long rate = denominator <= 0 ? SimConstant.Common.DASHBOARD_RATE_BASE
                : interactions * SimConstant.Common.DASHBOARD_RATE_BASE / denominator;
        int result = (int) Math.min(SimConstant.Common.DASHBOARD_RATE_BASE, rate);
        if (satisfactionLogEnabled && log.isInfoEnabled()) {
            //直接记录本次计算快照，不重复统计；人数不使用前端容纳展示的截断值。
            log.info("看板满意度计算：玩家编号：{}，场景编号：{}，统计时长：最近{}分钟，窗口开始：{}，窗口结束：{}，"
                            + "游客人数（未截断）：{}，规划交互次数：{}，配置要求交互次数：{}，每名游客要求交互次数：{}，"
                            + "最终满意度：{}%，计算说明：{}",
                    playerId, casino.getCasinoId(), SimConstant.Common.CAPACITY_WINDOW_MS / 60_000,
                    SATISFACTION_LOG_TIME.format(Instant.ofEpochMilli(now - SimConstant.Common.CAPACITY_WINDOW_MS)),
                    SATISFACTION_LOG_TIME.format(Instant.ofEpochMilli(now)),
                    guestCount, interactions, configuredInteractions, requiredInteractions,
                    BigDecimal.valueOf(result, 2).toPlainString(),
                    guestCount <= 0 ? "窗口内没有游客，默认满满意度"
                            : configuredInteractions <= 0 ? "配置未设置有效次数，按每名游客一次计算，最高百分之百"
                            : "规划交互次数除以游客人数与要求次数的乘积，最高百分之百");
        }
        return result;
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
