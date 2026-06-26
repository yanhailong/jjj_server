package com.jjg.game.sim.service;

import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.social.data.SendGiftConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    //CasinoStatsSheet配置 regionID -> level -> cfg
    private Map<Integer, Map<Integer, CasinoStatsSheetCfg>> casinoStatsSheetCfgMap;
    //ResearchInstitute配置 regionID -> gameTypeSet
    private Map<Integer, Set<Integer>> unlockGamesMap;

    //VisitorQuest配置 itemId -> cfg
    private Map<Integer, VisitorQuestCfg> visitorQuestItemCfgMap;
    //VisitorQuest配置 quality -> cfg
    private Map<Integer, List<VisitorQuestCfg>> visitorQuestCfgMap;
    //VisitorLevel配置 guestId -> level -> cfg
    private Map<Integer, Map<Integer, VisitorLevelCfg>> visitorLevelCfgMap;
    //VisitorStar配置 guestId -> star -> cfg
    private Map<Integer, Map<Integer, VisitorStarCfg>> visitorStarCfgMap;
    //visitorPool 的drop item的权重
    private Map<Integer, WeightRandom<List<Integer>>> visitorPoolRandomMap;
    //游客羁绊 guestId -> bondsCfgId
    private Map<Integer, Set<Integer>> visitorBondsMap = null;


    //EmployeeLevel配置 employeeId -> level -> cfg
    private Map<Integer, Map<Integer, EmployeeLevelCfg>> employeeLevelCfgMap;
    //EmployeeStar配置 employeeId -> star -> cfg
    private Map<Integer, Map<Integer, EmployeeStarCfg>> employeeStarCfgMap;
    //EmployeeProfile配置 道具id(角色道具) -> cfg
    private Map<Integer, EmployeeProfileCfg> employeeProfileItemCfgMap;
    //employeePool 的drop item的权重
    private Map<Integer, WeightRandom<List<Integer>>> employeePoolRandomMap;

    //建筑解锁链 casinoId -> type -> sequenceId升序的建筑ID列表
    private Map<Integer, Map<Integer, List<Integer>>> buildingChainMap;
    //建筑等级配置 buildingId -> level -> cfg
    private Map<Integer, Map<Integer, BuildingUpgradeTableCfg>> buildingUpgradeCfgMap;
    //建筑设备列表 buildingId -> 该建筑下所有设备 (EquipmentTable type==设备)
    private Map<Integer, List<Integer>> buildingDeviceMap;

    //技能配置
    private Map<Integer, List<PropCfg>> propCfgMap;

    //广告收益倍数随机
    private WeightRandom<String> adMultiplierRandom = null;

    //好友赠送礼物配置
    private SendGiftConfig sendGiftConfig;

    public void testInit() {
        loadCasinoStatsSheetCfg();
        loadResearchInstituteCfg();

        loadBuildingChain();
        loadBuildingDeviceConfig();
        loadBuildingUpgradeConfig();

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
    }

    /**
     * 加载 VisitorLevel 配置
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
        Map<Integer, Set<Integer>> tmpUnlockGamesMap = new HashMap<>();
        for (ResearchInstituteCfg cfg : GameDataManager.getResearchInstituteCfgList()) {
            tmpUnlockGamesMap.computeIfAbsent(cfg.getRegionID(), k -> new HashSet<>()).add(cfg.getGameType());
        }
        this.unlockGamesMap = tmpUnlockGamesMap;
    }

    /**
     * 加载VisitorQuestCfg
     */
    private void loadVisitorQuestConfig() {
        Map<Integer, List<VisitorQuestCfg>> tmpVisitorQuestCfgMap = new HashMap<>();
        Map<Integer, VisitorQuestCfg> tmpVisitorQuestItemCfgMap = new HashMap<>();
        for (VisitorQuestCfg cfg : GameDataManager.getVisitorQuestCfgList()) {
            tmpVisitorQuestCfgMap.computeIfAbsent(cfg.getQuality(), k -> new ArrayList<>()).add(cfg);

            List<Integer> tmpList = cfg.getDuplicatetoShard();
            if (tmpList != null && tmpList.size() >= 3) {
                tmpVisitorQuestItemCfgMap.put(tmpList.getFirst(), cfg);
            }
        }
        this.visitorQuestCfgMap = tmpVisitorQuestCfgMap;
        this.visitorQuestItemCfgMap = tmpVisitorQuestItemCfgMap;
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
     * 加载建筑解锁链
     * casinoId -> type -> 按 SequenceID 升序的 buildingId 列表
     */
    private void loadBuildingChain() {
        Map<Integer, Map<Integer, List<int[]>>> raw = new HashMap<>();
        for (BuildingAreaTableCfg cfg : GameDataManager.getBuildingAreaTableCfgList()) {
            raw.computeIfAbsent(cfg.getRegionID(), k -> new HashMap<>())
                    .computeIfAbsent(cfg.getType(), k -> new ArrayList<>())
                    .add(new int[]{cfg.getSequenceID(), cfg.getId()});
        }
        Map<Integer, Map<Integer, List<Integer>>> tmp = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, List<int[]>>> e1 : raw.entrySet()) {
            Map<Integer, List<Integer>> typeMap = new HashMap<>();
            for (Map.Entry<Integer, List<int[]>> e2 : e1.getValue().entrySet()) {
                e2.getValue().sort((a, b) -> Integer.compare(a[0], b[0]));
                List<Integer> ordered = new ArrayList<>(e2.getValue().size());
                for (int[] pair : e2.getValue()) {
                    ordered.add(pair[1]);
                }
                typeMap.put(e2.getKey(), Collections.unmodifiableList(ordered));
            }
            tmp.put(e1.getKey(), typeMap);
        }
        this.buildingChainMap = tmp;
    }

    /**
     * 加载建筑等级配置
     */
    private void loadBuildingUpgradeConfig() {
        Map<Integer, Map<Integer, BuildingUpgradeTableCfg>> tmp = new HashMap<>();
        for (BuildingUpgradeTableCfg cfg : GameDataManager.getBuildingUpgradeTableCfgList()) {
            tmp.computeIfAbsent(cfg.getBuildingID(), k -> new HashMap<>()).put(cfg.getLevel(), cfg);
        }
        this.buildingUpgradeCfgMap = tmp;
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
            if(arr.length == 4){
                this.sendGiftConfig = new SendGiftConfig(Integer.parseInt(arr[0]), Long.parseLong(arr[1]), Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
            }
        }
    }

    private void loadPropConfig() {
        Map<Integer, List<PropCfg>> tmpMap = new HashMap<>();
        for (PropCfg cfg : GameDataManager.getPropCfgList()) {
            tmpMap.computeIfAbsent(cfg.getGameType(), k -> new ArrayList<>()).add(cfg);
        }
        this.propCfgMap = tmpMap;
    }

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(CasinoStatsSheetCfg.EXCEL_NAME, this::loadCasinoStatsSheetCfg);
        addInitSampleFileObserveWithCallBack(ResearchInstituteCfg.EXCEL_NAME, this::loadResearchInstituteCfg);

        addInitSampleFileObserveWithCallBack(VisitorQuestCfg.EXCEL_NAME, this::loadVisitorQuestConfig);
        addInitSampleFileObserveWithCallBack(VisitorLevelCfg.EXCEL_NAME, this::loadVisitorLevelConfig);
        addInitSampleFileObserveWithCallBack(VisitorStarCfg.EXCEL_NAME, this::loadVisitorStarConfig);
        addInitSampleFileObserveWithCallBack(VisitorPoolCfg.EXCEL_NAME, this::loadVisitorPoolConfig);
        addInitSampleFileObserveWithCallBack(VisitorBondsCfg.EXCEL_NAME, this::loadVisitorBondsConfig);

        addInitSampleFileObserveWithCallBack(EmployeeLevelCfg.EXCEL_NAME, this::loadEmployeeLevelConfig);
        addInitSampleFileObserveWithCallBack(EmployeeStarCfg.EXCEL_NAME, this::loadEmployeeStarConfig);
        addInitSampleFileObserveWithCallBack(EmployeeProfileCfg.EXCEL_NAME, this::loadEmployeeProfileConfig);
        addInitSampleFileObserveWithCallBack(EmployeePoolCfg.EXCEL_NAME, this::loadEmployeePoolConfig);

        addInitSampleFileObserveWithCallBack(BuildingAreaTableCfg.EXCEL_NAME, this::loadBuildingChain);
        addInitSampleFileObserveWithCallBack(BuildingUpgradeTableCfg.EXCEL_NAME, this::loadBuildingUpgradeConfig);
        addInitSampleFileObserveWithCallBack(BuildingEquipmentTableCfg.EXCEL_NAME, this::loadBuildingDeviceConfig);
        addInitSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::loadGlobalConfig);

        addInitSampleFileObserveWithCallBack(PropCfg.EXCEL_NAME, this::loadPropConfig);
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
     * 获取建筑解锁链中下一个待解锁的 buildingId
     */
    public Integer getNextUnlockBuilding(int casinoId, int type, java.util.Set<Integer> unlocked) {
        if (buildingChainMap == null) {
            return null;
        }
        Map<Integer, List<Integer>> typeMap = buildingChainMap.get(casinoId);
        if (typeMap == null) {
            return null;
        }
        List<Integer> chain = typeMap.get(type);
        if (chain == null) {
            return null;
        }
        for (Integer id : chain) {
            if (!unlocked.contains(id)) {
                return id;
            }
        }
        return null;
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

    public Set<Integer> getUnlockGameByRegionId(int regionId) {
        if (this.unlockGamesMap == null) {
            return Collections.emptySet();
        }
        return unlockGamesMap.get(regionId);
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
}
