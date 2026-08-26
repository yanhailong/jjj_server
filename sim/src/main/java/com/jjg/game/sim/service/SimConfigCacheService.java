package com.jjg.game.sim.service;

import com.jjg.game.alliance.data.AllianceRefreshTaskConfig;
import com.jjg.game.alliance.data.DonateCfg;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.base.condition.numeric.ConditionRuleRegistry;
import com.jjg.game.core.base.condition.numeric.ConditionSpec;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.BuildingUnlockEquipmentData;
import com.jjg.game.sim.pb.struct.RecruitPoolInfo;
import com.jjg.game.social.data.SendGiftConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 配置缓存
 *
 * @author 11
 * @date 2026/5/26
 */
@Component
public class SimConfigCacheService implements ConfigExcelChangeListener {
    private static final Logger log = LoggerFactory.getLogger(SimConfigCacheService.class);
    private static final DateTimeFormatter POOL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("[yyyy-MM-dd HH:mm:ss][yyyy/MM/dd HH:mm:ss]");

    //CasinoStatsSheet配置 regionID -> level -> cfg
    private Map<Integer, Map<Integer, CasinoStatsSheetCfg>> casinoStatsSheetCfgMap;
    //ResearchInstitute配置 regionID -> 可研发的gameType集合
    private Map<Integer, Set<Integer>> researchGamesMap;
    //客座赌局试玩场次 gameType(=warehouse.gameID) -> 单人slots场次id, 供拜访试玩进房
    private Map<Integer, Integer> trialWareMap;

    //VisitorQuest配置 itemId -> cfg
    private Map<Integer, VisitorQuestCfg> visitorQuestItemCfgMap;
    //VisitorQuest配置 quality -> cfg
    private Map<Integer, List<VisitorQuestCfg>> visitorQuestCfgMap;
    //VisitorLevel配置 guestId -> level -> cfg
    private Map<Integer, Map<Integer, VisitorLevelCfg>> visitorLevelCfgMap;
    //VisitorStar配置 guestId -> star -> cfg
    private Map<Integer, Map<Integer, VisitorStarCfg>> visitorStarCfgMap;
    //VisitorStar配置 regionId -> cfg
    private Map<Integer, List<VisitorQuestCfg>> regionVistorCfgMap;
    //visitorPool 的drop item的权重
    private Map<Integer, WeightRandom<List<Integer>>> visitorPoolRandomMap;
    //游客羁绊 guestId -> bondsCfgId
    private Map<Integer, Set<Integer>> visitorBondsMap = null;
    //游客品质道具对应
    private Map<Integer, Integer> guestQulityItemMap = new HashMap<>();


    //EmployeeLevel配置 employeeId -> level -> cfg
    private Map<Integer, Map<Integer, EmployeeLevelCfg>> employeeLevelCfgMap;
    //EmployeeStar配置 employeeId -> star -> cfg
    private Map<Integer, Map<Integer, EmployeeStarCfg>> employeeStarCfgMap;
    //EmployeeProfile配置 道具id(角色道具) -> cfg
    private Map<Integer, EmployeeProfileCfg> employeeProfileItemCfgMap;
    //employeePool 的drop item的权重
    private Map<Integer, WeightRandom<List<Integer>>> employeePoolRandomMap;

    //建筑等级配置 buildingId -> level -> cfg
    private Map<Integer, Map<Integer, BuildingUpgradeTableCfg>> buildingUpgradeCfgMap;
    //建筑等级解锁设备的信息
    private Map<Integer, BuildingUnlockEquipmentData> buildingUnlockEquipmentDataMap;

    //建筑设备列表 buildingId -> 该建筑下所有设备 (EquipmentTable type==设备)
    private Map<Integer, List<Integer>> buildingDeviceMap;
    //gameType -> BuildingAreaTableCfg
    private Map<Integer, BuildingAreaTableCfg> gameBuildingAreaTableCfg;

    //技能配置
    private Map<Integer, List<PropCfg>> propCfgMap;

    //广告收益倍数随机
    private WeightRandom<String> adMultiplierRandom = null;

    //好友赠送礼物配置
    private SendGiftConfig sendGiftConfig;

    //联盟等级
    private Map<Integer, AllianceLevelCfg> allianceLevelCfgMap;
    // 热更新时整体替换不可变快照；volatile 保证配置线程向玩家业务线程安全发布。
    private volatile List<TaskCfg> allianceTasks;
    private volatile Map<Integer, TaskCfg> allianceTaskMap;
    private volatile Map<Integer, PreparedCondition> allianceTaskConditionMap;
    //联盟捐献配置
    private DonateCfg allianceDonateCfg;
    //联盟刷新任务配置
    private AllianceRefreshTaskConfig allianceRefreshTaskConfig;
    //创建联盟需要消耗的道具
    private Item createAllianceItem;

    //研究点道具 gameType -> ItemCfg
    private Map<Integer, ItemCfg> researchPointsItemCfgMap = Collections.emptyMap();

    //赛季匹配模拟数据
    private Map<Integer, List<SeasonSimulationDataCfg>> seasonSimulationDataCfgMap;

    private final ConditionRuleRegistry conditionRules;

    //赛季结算赛季币返还: [0]返还比例(百分比), [1]返还上限
    private int[] seasonReturnMaxArr = new int[2];

    private Set<Integer> genGuestGuideSet = null;

    @Autowired
    public SimConfigCacheService(ConditionRuleRegistry conditionRules) {
        this.conditionRules = conditionRules;
    }

    public void init() {
        initGuestQualityItems();
    }


    public void testLoadConfig() {
        loadCasinoStatsSheetCfg();
        loadResearchInstituteCfg();
        loadTrialWareConfig();

        loadBuildingDeviceConfig();
        loadBuildingUpgradeConfig();
        loadBuildingAreaTableConfig();

        loadEmployeeLevelConfig();
        loadEmployeeStarConfig();
        loadEmployeeProfileConfig();
        loadEmployeePoolConfig();

        loadVisitorQuestConfig();
        loadVisitorLevelConfig();
        loadVisitorStarConfig();
        loadVisitorPoolConfig();
        loadVisitorBondsConfig();

        loadGlobalConfig();

        loadPropConfig();

        loadAllianceLevelConfig();
        loadAllianceTasks();

        loadItemConfig();
        loadSeasonSimulationDataConfig();
    }

    /**
     * 将游客道具id和品质id对应
     */
    private void initGuestQualityItems() {
        this.guestQulityItemMap.put(SimConstant.Item.ID_GUEST_QULITY_WHITE, 1);
        this.guestQulityItemMap.put(SimConstant.Item.ID_GUEST_QULITY_GREEN, 2);
        this.guestQulityItemMap.put(SimConstant.Item.ID_GUEST_QULITY_BLUE, 3);
        this.guestQulityItemMap.put(SimConstant.Item.ID_GUEST_QULITY_PUEPLE, 4);
        this.guestQulityItemMap.put(SimConstant.Item.ID_GUEST_QULITY_GOLD, 5);
    }

    /**
     * 加载各场景可研发的游戏
     */
    private void loadCasinoStatsSheetCfg() {
        Map<Integer, Map<Integer, CasinoStatsSheetCfg>> tmp = new HashMap<>();
        for (CasinoStatsSheetCfg cfg : GameDataManager.getCasinoStatsSheetCfgList()) {
            tmp.computeIfAbsent(cfg.getRegionID(), k -> new HashMap<>()).put(cfg.getLevel(), cfg);
        }
        this.casinoStatsSheetCfgMap = tmp;
    }

    /**
     * 加载 VisitorLevel 配置
     */
    private void loadResearchInstituteCfg() {
        Map<Integer, Set<Integer>> tmpResearchGamesMap = new HashMap<>();
        for (ResearchInstituteCfg cfg : GameDataManager.getResearchInstituteCfgList()) {
            tmpResearchGamesMap.computeIfAbsent(cfg.getRegionID(), k -> new HashSet<>()).add(cfg.getGameType());
        }
        this.researchGamesMap = tmpResearchGamesMap;
    }

    /**
     * 加载客座赌局试玩场次: gameID -> 单人slots场次(roomType<好友房起始, 取最小场次id保证确定性)。
     * 拜访试玩进房用: 访客按房主已解锁的游戏进入该游戏的普通单人场次, 属性走房主研发(技能在slots侧按房主取)。
     */
    private void loadTrialWareConfig() {
        Map<Integer, Integer> tmp = new HashMap<>();
        for (WarehouseCfg cfg : GameDataManager.getWarehouseCfgList()) {
            if (cfg.getRoomType() >= GameConstant.RoomTypeCons.FRIEND_ROOM_TYPE_START) {
                continue;
            }
            tmp.merge(cfg.getGameID(), cfg.getId(), Math::min);
        }
        this.trialWareMap = tmp;
    }

    /**
     * 加载VisitorQuestCfg
     */
    private void loadVisitorQuestConfig() {
        Map<Integer, List<VisitorQuestCfg>> tmpVisitorQuestCfgMap = new HashMap<>();
        Map<Integer, VisitorQuestCfg> tmpVisitorQuestItemCfgMap = new HashMap<>();
        Map<Integer, List<VisitorQuestCfg>> tmpRegionVistorCfgMap = new HashMap<>();
        for (VisitorQuestCfg cfg : GameDataManager.getVisitorQuestCfgList()) {
            tmpVisitorQuestCfgMap.computeIfAbsent(cfg.getQuality(), k -> new ArrayList<>()).add(cfg);

            List<Integer> tmpList = cfg.getDuplicatetoShard();
            if (tmpList != null && tmpList.size() >= 3) {
                tmpVisitorQuestItemCfgMap.put(tmpList.getFirst(), cfg);
            }

            tmpRegionVistorCfgMap.computeIfAbsent(cfg.getRegionID(), k -> new ArrayList<>()).add(cfg);
        }
        this.visitorQuestCfgMap = tmpVisitorQuestCfgMap;
        this.visitorQuestItemCfgMap = tmpVisitorQuestItemCfgMap;
        this.regionVistorCfgMap = tmpRegionVistorCfgMap;
    }

    /**
     * 加载 VisitorLevel 配置
     */
    private void loadVisitorLevelConfig() {
        Map<Integer, Map<Integer, VisitorLevelCfg>> tmp = new HashMap<>();
        for (VisitorLevelCfg cfg : GameDataManager.getVisitorLevelCfgList()) {
            tmp.computeIfAbsent(cfg.getVisitor(), k -> new HashMap<>()).put(cfg.getLevel(), cfg);
        }
        this.visitorLevelCfgMap = tmp;
    }

    /**
     * 加载 VisitorStar 配置
     */
    private void loadVisitorStarConfig() {
        Map<Integer, Map<Integer, VisitorStarCfg>> tmp = new HashMap<>();
        for (VisitorStarCfg cfg : GameDataManager.getVisitorStarCfgList()) {
            tmp.computeIfAbsent(cfg.getVisitor(), k -> new HashMap<>()).put(cfg.getStarlevel(), cfg);
        }
        this.visitorStarCfgMap = tmp;
    }

    /**
     * 加载 VisitorPool 配置
     */
    private void loadVisitorPoolConfig() {
        Map<Integer, WeightRandom<List<Integer>>> tmpVisitorPoolRandomMap = new HashMap<>();
        for (VisitorPoolCfg cfg : GameDataManager.getVisitorPoolCfgList()) {
            WeightRandom<List<Integer>> random = WeightRandom.create();
            for (List<Integer> list : cfg.getDetailedDropItem()) {
                random.add(list, list.getFirst());
            }
            tmpVisitorPoolRandomMap.put(cfg.getId(), random);
        }
        this.visitorPoolRandomMap = tmpVisitorPoolRandomMap;
    }

    private void loadVisitorBondsConfig() {
        Map<Integer, Set<Integer>> tmpVisitorBondsMap = new HashMap<>();
        for (VisitorBondsCfg cfg : GameDataManager.getVisitorBondsCfgList()) {
            if (cfg.getMembers() == null || cfg.getMembers().isEmpty()) {
                continue;
            }
            for (int memberGuestId : cfg.getMembers()) {
                tmpVisitorBondsMap.computeIfAbsent(memberGuestId, k -> new HashSet<>()).add(cfg.getId());
            }
        }
        this.visitorBondsMap = tmpVisitorBondsMap;
    }

    /**
     * 加载 EmployeeLevel 配置
     */
    private void loadEmployeeLevelConfig() {
        Map<Integer, Map<Integer, EmployeeLevelCfg>> tmp = new HashMap<>();
        for (EmployeeLevelCfg cfg : GameDataManager.getEmployeeLevelCfgList()) {
            tmp.computeIfAbsent(cfg.getEmployeeID(), k -> new HashMap<>()).put(cfg.getLevel(), cfg);
        }
        this.employeeLevelCfgMap = tmp;
    }

    /**
     * 加载 EmployeeStar 配置
     */
    private void loadEmployeeStarConfig() {
        Map<Integer, Map<Integer, EmployeeStarCfg>> tmp = new HashMap<>();
        for (EmployeeStarCfg cfg : GameDataManager.getEmployeeStarCfgList()) {
            tmp.computeIfAbsent(cfg.getEmployeeID(), k -> new HashMap<>()).put(cfg.getStar(), cfg);
        }
        this.employeeStarCfgMap = tmp;
    }

    /**
     * 加载 EmployeeProfile 配置 (按角色道具id索引, 用于卡池招募)
     */
    private void loadEmployeeProfileConfig() {
        Map<Integer, EmployeeProfileCfg> tmp = new HashMap<>();
        for (EmployeeProfileCfg cfg : GameDataManager.getEmployeeProfileCfgList()) {
            List<Integer> shard = cfg.getDuplicatetoShard();
            if (shard != null && shard.size() >= 3) {
                tmp.put(shard.getFirst(), cfg);
            }
        }
        this.employeeProfileItemCfgMap = tmp;
    }

    /**
     * 加载 EmployeePool 配置
     */
    private void loadEmployeePoolConfig() {
        Map<Integer, WeightRandom<List<Integer>>> tmp = new HashMap<>();
        for (EmployeePoolCfg cfg : GameDataManager.getEmployeePoolCfgList()) {
            WeightRandom<List<Integer>> random = WeightRandom.create();
            for (List<Integer> list : cfg.getDetailedDropItem()) {
                random.add(list, list.getFirst());
            }
            tmp.put(cfg.getId(), random);
        }
        this.employeePoolRandomMap = tmp;
    }

    /**
     * 加载建筑等级配置
     */
    private void loadBuildingUpgradeConfig() {
        Map<Integer, Map<Integer, BuildingUpgradeTableCfg>> tmp = new HashMap<>();
        Map<Integer, BuildingUnlockEquipmentData> tmpBuildingUnlockEquipmentDataMap = new HashMap<>();
        for (BuildingUpgradeTableCfg cfg : GameDataManager.getBuildingUpgradeTableCfgList()) {
            tmp.computeIfAbsent(cfg.getBuildingID(), k -> new HashMap<>()).put(cfg.getLevel(), cfg);

            BuildingUnlockEquipmentData buildingUnlockEquipmentData = tmpBuildingUnlockEquipmentDataMap.computeIfAbsent(cfg.getBuildingID(), k -> {
                BuildingUnlockEquipmentData data = new BuildingUnlockEquipmentData();
                data.setBuildId(cfg.getBuildingID());
                return data;
            });

            if (cfg.getUnlockEquipment() != null && !cfg.getUnlockEquipment().isEmpty()) {
                if (buildingUnlockEquipmentData.getMaxLevel() < cfg.getLevel()) {
                    buildingUnlockEquipmentData.setMaxLevel(cfg.getLevel());
                }

                List<Integer> tmpList = buildingUnlockEquipmentData.getLevelUnlockEquipment(cfg.getLevel() - 1);
                List<Integer> allList = new ArrayList<>();
                if (tmpList != null) {
                    allList.addAll(tmpList);
                }
                allList.addAll(new HashSet<>(cfg.getUnlockEquipment()));
                buildingUnlockEquipmentData.setLevelUnlockEquipment(cfg.getLevel(), allList);
            }

        }
        this.buildingUpgradeCfgMap = tmp;
        this.buildingUnlockEquipmentDataMap = tmpBuildingUnlockEquipmentDataMap;
    }

    private void loadBuildingAreaTableConfig() {
        Map<Integer, BuildingAreaTableCfg> tmpGameBuildingAreaTableCfg = new HashMap<>();
        for (BuildingAreaTableCfg cfg : GameDataManager.getBuildingAreaTableCfgList()) {
            if (cfg.getUnlockGameId() > 0) {
                tmpGameBuildingAreaTableCfg.put(cfg.getUnlockGameId(), cfg);
            }
        }
        this.gameBuildingAreaTableCfg = tmpGameBuildingAreaTableCfg;
    }

    /**
     * 加载建筑设备列表
     */
    private void loadBuildingDeviceConfig() {
        Map<Integer, List<Integer>> tmp = new HashMap<>();
        for (BuildingEquipmentTableCfg cfg : GameDataManager.getBuildingEquipmentTableCfgList()) {
            if (cfg.getType() != SimConstant.EquipmentType.DEVICE) {
                continue;
            }
            tmp.computeIfAbsent(cfg.getBuildingID(), k -> new ArrayList<>()).add(cfg.getId());
        }
        this.buildingDeviceMap = tmp;
    }

    /**
     * 加载全局配置
     */
    private void loadGlobalConfig() {
        //广告收益倍数配置
        GlobalConfigCfg adCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.GLOBAL_AD_MULTIPLIER_ID);
        if (adCfg != null && adCfg.getValue() != null && !adCfg.getValue().isEmpty()) {
            WeightRandom<String> random = WeightRandom.create();
            for (String seg : adCfg.getValue().split("\\|")) {
                String[] kv = seg.split("_");
                if (kv.length < 2) {
                    continue;
                }
                try {
                    Double.parseDouble(kv[0].trim());
                    int weight = Integer.parseInt(kv[1].trim());
                    if (weight > 0) {
                        random.add(kv[0].trim(), weight);
                    }
                } catch (NumberFormatException e) {
                    log.warn("广告倍数配置解析失败 seg={}", seg);
                }
            }

            this.adMultiplierRandom = random;
        }

        //好友聊天：每日赠送礼物详情
        GlobalConfigCfg giftCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.SOCIAL_SEND_GIFT_ID);
        if (giftCfg != null && giftCfg.getValue() != null && !giftCfg.getValue().isEmpty()) {
            String[] arr = giftCfg.getValue().split("_");
            if (arr.length == 4) {
                this.sendGiftConfig = new SendGiftConfig(Integer.parseInt(arr[0]), Long.parseLong(arr[1]), Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
            }
        }

        //联盟捐献
        DonateCfg tmpAllianceDonateCfg = new DonateCfg();
        GlobalConfigCfg allianceDailyDonateCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.ALLIANCE_DAILY_DONATE_ID);
        GlobalConfigCfg allianceDonateItemsCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.ALLIANCE_DONATE_ITEMS_ID);
        if (allianceDonateItemsCfg != null && allianceDonateItemsCfg.getValue() != null && !allianceDonateItemsCfg.getValue().isEmpty()) {
            String[] split = allianceDonateItemsCfg.getValue().split("_");
            tmpAllianceDonateCfg.setItemId(Integer.parseInt(split[0]));

            String[] split1 = split[1].split(",");
            List<Long> counts = new ArrayList<>();
            for (String s : split1) {
                counts.add(Long.parseLong(s));
            }
            tmpAllianceDonateCfg.setCounts(counts);
        }
        GlobalConfigCfg allianceDonateRewardsCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.ALLIANCE_DONATE_REWARD_ID);
        GlobalConfigCfg allianceDonateRewardsReputationCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.ALLIANCE_DONATE_REPUTATION_ID);
        tmpAllianceDonateCfg.setMemberDailyLimit(allianceDailyDonateCfg.getIntValue());
        tmpAllianceDonateCfg.setRewardContribution(allianceDonateRewardsCfg.getIntValue());
        tmpAllianceDonateCfg.setRewardReputation(allianceDonateRewardsReputationCfg.getIntValue());
        this.allianceDonateCfg = tmpAllianceDonateCfg;

        //联盟每日任务刷新
        GlobalConfigCfg aRefreshTaskCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.ALLIANCE_DAILY_FRESH_TASK);
        if (aRefreshTaskCfg != null && aRefreshTaskCfg.getValue() != null && !aRefreshTaskCfg.getValue().isEmpty()) {
            AllianceRefreshTaskConfig tmpAllianceRefreshTaskConfig = new AllianceRefreshTaskConfig();
            String[] s = aRefreshTaskCfg.getValue().split("_");
            tmpAllianceRefreshTaskConfig.setItemId(Integer.parseInt(s[0]));
            tmpAllianceRefreshTaskConfig.setDailyCountLimit(Integer.parseInt(s[1]));
            tmpAllianceRefreshTaskConfig.setSpendCountEach(Integer.parseInt(s[2]));
            this.allianceRefreshTaskConfig = tmpAllianceRefreshTaskConfig;
        }

        //联盟每日任务刷新
        GlobalConfigCfg createAllianceCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.ALLIANCE_CREATE_ALLIANCE_CFG_ID);
        if (createAllianceCfg != null) {
            String[] s = createAllianceCfg.getValue().split("_");
            this.createAllianceItem = new Item(Integer.parseInt(s[0]), Long.parseLong(s[1]));
        }

        //赛季币返还比例与上限
        GlobalConfigCfg seasonReturnMaxArrCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Global.ID_RETURN_COIN_MAX);
        if (seasonReturnMaxArrCfg != null) {
            String[] s = seasonReturnMaxArrCfg.getValue().split("_");
            int[] tmpSeasonReturnMaxArr = new int[2];
            //返还的比例
            tmpSeasonReturnMaxArr[0] = Integer.parseInt(s[0]);
            //返还的上限
            tmpSeasonReturnMaxArr[1] = Integer.parseInt(s[1]);
            this.seasonReturnMaxArr = tmpSeasonReturnMaxArr;
        }

        //完成这些新手引导才能生成游客
        GlobalConfigCfg genGuestGuideCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Global.GEN_GUEST_GUIDE);
        if (genGuestGuideCfg != null) {
            Set<Integer> tmpGenGuestGuideSet = new HashSet<>();
            String[] s = genGuestGuideCfg.getValue().split("_");
            for (String s1 : s) {
                tmpGenGuestGuideSet.add(Integer.parseInt(s1));
            }
            this.genGuestGuideSet = tmpGenGuestGuideSet;
        }
    }

    private void loadPropConfig() {
        Map<Integer, List<PropCfg>> tmpMap = new HashMap<>();
        for (PropCfg cfg : GameDataManager.getPropCfgList()) {
            int gameType = cfg.getType() == 1 ? 0 : cfg.getGameType();
            tmpMap.computeIfAbsent(gameType, k -> new ArrayList<>()).add(cfg);
        }
        this.propCfgMap = tmpMap;
    }

    private void loadAllianceLevelConfig() {
        Map<Integer, AllianceLevelCfg> tmpAllianceLevelCfgMap = new HashMap<>();
        for (AllianceLevelCfg cfg : GameDataManager.getAllianceLevelCfgList()) {
            tmpAllianceLevelCfgMap.put(cfg.getLevel(), cfg);
        }
        this.allianceLevelCfgMap = tmpAllianceLevelCfgMap;
    }

    private void loadAllianceTasks() {
        List<TaskCfg> all = GameDataManager.getTaskCfgList();
        if (all == null || all.isEmpty()) {
            allianceTasks = Collections.emptyList();
            allianceTaskMap = Collections.emptyMap();
            allianceTaskConditionMap = Collections.emptyMap();
            return;
        }
        List<TaskCfg> tasks = new ArrayList<>();
        Map<Integer, TaskCfg> map = new HashMap<>();
        Map<Integer, PreparedCondition> conditionMap = new HashMap<>();
        for (TaskCfg cfg : all) {
            if (cfg != null && cfg.getTaskType() == TaskConstant.TaskType.ALLIANCE) {
                PreparedCondition condition;
                try {
                    condition = conditionRules.prepare(ConditionSpec.from(cfg.getTaskConditionId()));
                } catch (IllegalArgumentException e) {
                    log.warn("联盟任务条件配置非法, 不入池 taskId={},condition={},error={}", cfg.getId(), cfg.getTaskConditionId(), e.getMessage());
                    continue;
                }
                tasks.add(cfg);
                map.put(cfg.getId(), cfg);
                conditionMap.put(cfg.getId(), condition);
            }
        }
        allianceTasks = Collections.unmodifiableList(tasks);
        allianceTaskMap = Collections.unmodifiableMap(map);
        allianceTaskConditionMap = Collections.unmodifiableMap(conditionMap);
    }

    private void loadItemConfig() {
        NumberFormat format = NumberFormat.getInstance();
        Map<Integer, ItemCfg> tmpResearchPointsItemCfgMap = new HashMap<>();
        try {
            for (Map.Entry<Integer, ItemCfg> en : GameDataManager.getItemCfgMap().entrySet()) {
                ItemCfg value = en.getValue();
                if (value.getItemType() == SimConstant.Item.ITEM_TYPE_RESEARCH_POINTS) {
                    if (StringUtils.isEmpty(value.getTargetCondition())) {
                        tmpResearchPointsItemCfgMap.put(0, value);
                    } else {
                        tmpResearchPointsItemCfgMap.put(format.parse(value.getTargetCondition()).intValue(), value);
                    }
                }
            }
        } catch (Exception e) {
            log.error("", e);
        }
        this.researchPointsItemCfgMap = Collections.unmodifiableMap(tmpResearchPointsItemCfgMap);
    }

    private void loadSeasonSimulationDataConfig() {
        Map<Integer, List<SeasonSimulationDataCfg>> tmpSeasonSimulationDataCfgMap = new HashMap<>();
        for (Map.Entry<Integer, SeasonSimulationDataCfg> en : GameDataManager.getSeasonSimulationDataCfgMap().entrySet()) {
            tmpSeasonSimulationDataCfgMap.computeIfAbsent(en.getValue().getType(), k -> new ArrayList<>()).add(en.getValue());
        }
        this.seasonSimulationDataCfgMap = tmpSeasonSimulationDataCfgMap;
    }

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(CasinoStatsSheetCfg.EXCEL_NAME, this::loadCasinoStatsSheetCfg);
        addInitSampleFileObserveWithCallBack(ResearchInstituteCfg.EXCEL_NAME, this::loadResearchInstituteCfg);
        addInitSampleFileObserveWithCallBack(WarehouseCfg.EXCEL_NAME, this::loadTrialWareConfig);

        addInitSampleFileObserveWithCallBack(VisitorQuestCfg.EXCEL_NAME, this::loadVisitorQuestConfig);
        addInitSampleFileObserveWithCallBack(VisitorLevelCfg.EXCEL_NAME, this::loadVisitorLevelConfig);
        addInitSampleFileObserveWithCallBack(VisitorStarCfg.EXCEL_NAME, this::loadVisitorStarConfig);
        addInitSampleFileObserveWithCallBack(VisitorPoolCfg.EXCEL_NAME, this::loadVisitorPoolConfig);
        addInitSampleFileObserveWithCallBack(VisitorBondsCfg.EXCEL_NAME, this::loadVisitorBondsConfig);

        addInitSampleFileObserveWithCallBack(EmployeeLevelCfg.EXCEL_NAME, this::loadEmployeeLevelConfig);
        addInitSampleFileObserveWithCallBack(EmployeeStarCfg.EXCEL_NAME, this::loadEmployeeStarConfig);
        addInitSampleFileObserveWithCallBack(EmployeeProfileCfg.EXCEL_NAME, this::loadEmployeeProfileConfig);
        addInitSampleFileObserveWithCallBack(EmployeePoolCfg.EXCEL_NAME, this::loadEmployeePoolConfig);

        addInitSampleFileObserveWithCallBack(BuildingUpgradeTableCfg.EXCEL_NAME, this::loadBuildingUpgradeConfig);
        addInitSampleFileObserveWithCallBack(BuildingEquipmentTableCfg.EXCEL_NAME, this::loadBuildingDeviceConfig);
        addInitSampleFileObserveWithCallBack(BuildingAreaTableCfg.EXCEL_NAME, this::loadBuildingAreaTableConfig);


        addInitSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::loadGlobalConfig);

        addInitSampleFileObserveWithCallBack(PropCfg.EXCEL_NAME, this::loadPropConfig);

        addInitSampleFileObserveWithCallBack(AllianceLevelCfg.EXCEL_NAME, this::loadAllianceLevelConfig);
        addInitSampleFileObserveWithCallBack(TaskCfg.EXCEL_NAME, this::loadAllianceTasks);

        addInitSampleFileObserveWithCallBack(ItemCfg.EXCEL_NAME, this::loadItemConfig);
        addInitSampleFileObserveWithCallBack(SeasonSimulationDataCfg.EXCEL_NAME, this::loadSeasonSimulationDataConfig);
    }

    // ---------------------------------------------------------------------
    // getters
    // ---------------------------------------------------------------------

    public Map<Integer, Map<Integer, VisitorLevelCfg>> getVisitorLevelCfgMap() {
        return visitorLevelCfgMap;
    }

    public Map<Integer, Map<Integer, VisitorStarCfg>> getVisitorStarCfgMap() {
        return visitorStarCfgMap;
    }

    public Map<Integer, Map<Integer, EmployeeLevelCfg>> getEmployeeLevelCfgMap() {
        return employeeLevelCfgMap;
    }

    public Map<Integer, Map<Integer, EmployeeStarCfg>> getEmployeeStarCfgMap() {
        return employeeStarCfgMap;
    }

    /**
     * 获取建筑等级配置
     */
    public BuildingUpgradeTableCfg getBuildingUpgradeCfg(int buildingId, int level) {
        if (buildingUpgradeCfgMap == null) {
            return null;
        }
        Map<Integer, BuildingUpgradeTableCfg> map = buildingUpgradeCfgMap.get(buildingId);
        return map == null ? null : map.get(level);
    }

    /**
     * 获取建筑的最大等级
     */
    public int getBuildingMaxLevel(int buildingId) {
        if (buildingUpgradeCfgMap == null) {
            return 0;
        }
        Map<Integer, BuildingUpgradeTableCfg> map = buildingUpgradeCfgMap.get(buildingId);
        if (map == null || map.isEmpty()) {
            return 0;
        }
        int max = 0;
        for (Integer l : map.keySet()) {
            if (l > max) {
                max = l;
            }
        }
        return max;
    }

    /**
     * 获取建筑下的设备列表
     */
    public List<Integer> getBuildingDevices(int buildingId) {
        if (buildingDeviceMap == null) {
            return Collections.emptyList();
        }
        List<Integer> list = buildingDeviceMap.get(buildingId);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * 广告收益倍数
     *
     * @return
     */
    public String pickAdMultiplier() {
        if (this.adMultiplierRandom == null) {
            return "1";
        }

        String next = this.adMultiplierRandom.next();
        if (next == null) {
            return "1";
        }
        return next;
    }

    public CasinoStatsSheetCfg getCasinoStatsSheetCfg(int regionId, int level) {
        if (this.casinoStatsSheetCfgMap == null) {
            return null;
        }
        Map<Integer, CasinoStatsSheetCfg> tmpMap = this.casinoStatsSheetCfgMap.get(regionId);
        if (tmpMap == null) {
            return null;
        }
        return tmpMap.get(level);
    }

    public Set<Integer> getResearchGamesByRegionId(int regionId) {
        if (this.researchGamesMap == null) {
            return Collections.emptySet();
        }
        return researchGamesMap.getOrDefault(regionId, Collections.emptySet());
    }

    /**
     * 客座赌局试玩进房场次: 该游戏(gameID)对应的单人slots场次id; 未配置返回 null
     */
    public Integer getTrialWareId(int gameType) {
        return trialWareMap == null ? null : trialWareMap.get(gameType);
    }

    public List<PropCfg> getPropCfgList(int gameType) {
        if (this.propCfgMap == null) {
            this.propCfgMap = new HashMap<>();
        }
        return this.propCfgMap.get(gameType);
    }

    public List<VisitorQuestCfg> getVisitorQuestCfgList(int quality) {
        if (this.visitorQuestCfgMap == null) {
            return null;
        }
        return this.visitorQuestCfgMap.get(quality);
    }

    public VisitorStarCfg getVisitorStarCfgByGuest(int guestId, int star) {
        if (this.visitorStarCfgMap == null || this.visitorStarCfgMap.isEmpty()) {
            return null;
        }
        Map<Integer, VisitorStarCfg> tmpMap = this.visitorStarCfgMap.get(guestId);
        if (tmpMap == null || tmpMap.isEmpty()) {
            return null;
        }
        return tmpMap.get(star);
    }

    public VisitorQuestCfg getVisitorQuestCfgByItemId(int itemId) {
        if (this.visitorQuestItemCfgMap == null || this.visitorQuestItemCfgMap.isEmpty()) {
            return null;
        }
        return this.visitorQuestItemCfgMap.get(itemId);
    }

    public List<RecruitPoolInfo> getOpenPoolIds(int poolType) {
        LocalDateTime now = LocalDateTime.now();
        List<RecruitPoolInfo> poolInfos = new ArrayList<>();
        for (PoolListCfg cfg : GameDataManager.getPoolListCfgList()) {
            if (!cfg.getOpen()) {
                continue;
            }
            if (cfg.getType() != poolType) {
                continue;
            }

            Integer endTimestamp = getPoolEndTimestamp(cfg, now);
            if (endTimestamp == null) {
                continue;
            }

            RecruitPoolInfo re = new RecruitPoolInfo();
            re.id = cfg.getId();
            re.endTime = endTimestamp;
            re.langId = cfg.getLanguageID();
            re.items = ItemUtils.buildItemInfo(cfg.getDrawCost());
            poolInfos.add(re);
        }
        return poolInfos;
    }

    private Integer getPoolEndTimestamp(PoolListCfg cfg, LocalDateTime now) {
        if (StringUtils.isEmpty(cfg.getTime_start()) || StringUtils.isEmpty(cfg.getTime_end())) {
            return 0;
        }

        String timeStart = cfg.getTime_start().trim();
        String timeEnd = cfg.getTime_end().trim();
        boolean startIsCron = CronExpression.isValidExpression(timeStart);
        boolean endIsCron = CronExpression.isValidExpression(timeEnd);
        if (startIsCron != endIsCron) {
            log.error("卡池开始和结束时间类型不一致 poolId={},timeStart={},timeEnd={}", cfg.getId(), timeStart, timeEnd);
            return null;
        }

        LocalDateTime startTime;
        LocalDateTime endTime;
        if (startIsCron) {
            CronExpression startCron = CronExpression.parse(timeStart);
            CronExpression endCron = CronExpression.parse(timeEnd);
            startTime = startCron.next(now.minusMonths(1));
            endTime = startTime == null ? null : endCron.next(startTime);
            while (endTime != null && !now.isBefore(endTime)) {
                startTime = startCron.next(endTime);
                endTime = startTime == null ? null : endCron.next(startTime);
            }
        } else {
            try {
                startTime = LocalDateTime.parse(timeStart, POOL_DATE_TIME_FORMATTER);
                endTime = LocalDateTime.parse(timeEnd, POOL_DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                log.error("卡池时间配置解析失败 poolId={},timeStart={},timeEnd={}", cfg.getId(), timeStart, timeEnd);
                return null;
            }
        }

        if (startTime == null || endTime == null || !startTime.isBefore(endTime)
                || now.isBefore(startTime) || !now.isBefore(endTime)) {
            return null;
        }
        return (int) (TimeHelper.getTimestamp(endTime) / TimeHelper.ONE_SECOND_OF_MILLIS);
    }

    public PoolListCfg getOpenPoolCfg(int poolId, int type) {
        PoolListCfg cfg = GameDataManager.getPoolListCfg(poolId);
        if (cfg == null || !cfg.getOpen() || cfg.getType() != type
                || getPoolEndTimestamp(cfg, LocalDateTime.now()) == null) {
            return null;
        }
        return cfg;
    }

    public WeightRandom<List<Integer>> getPoolRand(int cfgId) {
        if (this.visitorPoolRandomMap == null || this.visitorPoolRandomMap.isEmpty()) {
            return null;
        }
        return this.visitorPoolRandomMap.get(cfgId);
    }

    public EmployeeProfileCfg getEmployeeProfileCfgByItemId(int itemId) {
        if (this.employeeProfileItemCfgMap == null || this.employeeProfileItemCfgMap.isEmpty()) {
            return null;
        }
        return this.employeeProfileItemCfgMap.get(itemId);
    }

    public WeightRandom<List<Integer>> getEmployeePoolRand(int cfgId) {
        if (this.employeePoolRandomMap == null || this.employeePoolRandomMap.isEmpty()) {
            return null;
        }
        return this.employeePoolRandomMap.get(cfgId);
    }

    public Set<Integer> getBondsByGuestId(int guestId) {
        if (this.visitorBondsMap == null || this.visitorBondsMap.isEmpty()) {
            return null;
        }
        return this.visitorBondsMap.get(guestId);
    }

    public SendGiftConfig getSendGiftConfig() {
        return sendGiftConfig;
    }

    public AllianceLevelCfg allianceLevelCfg(int level) {
        if (this.allianceLevelCfgMap == null || this.allianceLevelCfgMap.isEmpty()) {
            return null;
        }
        return this.allianceLevelCfgMap.get(level);
    }

    /**
     * 按累计声誉重算联盟等级 (只升不降由调用方保证)。
     * <p>
     * {@link AllianceLevelCfg#getReputationRequired()} 语义为「从该等级升到下一级所需的累计声誉值」
     * (与 {@code AllianceBrief.nextLevelReputation} 一致), 因此达标后等级应为 {@code i + 1},
     * 而不是停留在 i。
     */
    public int allianceLevelOf(int nowLevel, int reputation) {
        if (this.allianceLevelCfgMap == null || this.allianceLevelCfgMap.isEmpty()) {
            return nowLevel;
        }
        int maxLevel = this.allianceLevelCfgMap.size();
        if (nowLevel >= maxLevel) {
            return nowLevel;
        }

        int newLevel = nowLevel;
        // 从当前等级起逐级判定: 达到本级升级门槛则升到下一级, 最多升到 maxLevel
        for (int i = nowLevel; i < maxLevel; i++) {
            AllianceLevelCfg cfg = this.allianceLevelCfgMap.get(i);
            if (cfg == null) {
                break;
            }
            if (reputation < cfg.getReputationRequired()) {
                break;
            }
            newLevel = i + 1;
        }
        return newLevel;
    }

    public TaskCfg getAllianceTaskByCfgId(int cfgId) {
        if (this.allianceTaskMap == null) {
            return null;
        }
        return this.allianceTaskMap.get(cfgId);
    }

    public PreparedCondition getAllianceTaskCondition(int cfgId) {
        return allianceTaskConditionMap == null ? null : allianceTaskConditionMap.get(cfgId);
    }

    public List<TaskCfg> randomAllianceTasks(int n, Set<Integer> exclude) {
        if (n <= 0 || allianceTasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<TaskCfg> candidates = new ArrayList<>(allianceTasks.size());
        for (TaskCfg cfg : allianceTasks) {
            if (exclude == null || !exclude.contains(cfg.getId())) {
                candidates.add(cfg);
            }
        }
        Collections.shuffle(candidates);
        return candidates.size() > n ? candidates.subList(0, n) : candidates;
    }

    public DonateCfg getAllianceDonateCfg() {
        return allianceDonateCfg;
    }

    public Integer queryGuestQuality(int itemId) {
        return this.guestQulityItemMap.get(itemId);
    }

    public AllianceRefreshTaskConfig getAllianceRefreshTaskConfig() {
        return allianceRefreshTaskConfig;
    }

    public Item getCreateAllianceItem() {
        return createAllianceItem;
    }

    public ItemCfg getResearchPointItemCfg(int gameType) {
        return researchPointsItemCfgMap.get(gameType);
    }

    public Map<Integer, List<SeasonSimulationDataCfg>> getSeasonSimulationDataCfgMap() {
        return seasonSimulationDataCfgMap;
    }

    public int[] getSeasonReturnMaxArr() {
        return seasonReturnMaxArr;
    }

    public Map<Integer, List<VisitorQuestCfg>> getRegionVistorCfgMap() {
        return regionVistorCfgMap;
    }

    public Set<Integer> getGenGuestGuideSet() {
        return genGuestGuideSet;
    }

    public BuildingAreaTableCfg getBuildingAreaTableCfgByGameType(int gameType) {
        if (gameType < 1) {
            return null;
        }
        return this.gameBuildingAreaTableCfg.get(gameType);
    }

    public BuildingUnlockEquipmentData getBuildingUnlockEquipmentDataByBuildId(int buildId) {
        if (buildingUnlockEquipmentDataMap == null) {
            return null;
        }
        return this.buildingUnlockEquipmentDataMap.get(buildId);
    }
}
