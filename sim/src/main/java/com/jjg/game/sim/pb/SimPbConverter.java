package com.jjg.game.sim.pb;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sim.constant.ServerBuildingType;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.pb.struct.*;
import com.jjg.game.sim.service.SimBuildingService;
import com.jjg.game.sim.service.SimConfigCacheService;
import com.jjg.game.sim.service.SimOperationDashboardService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据 → 协议 转换工具
 *
 * @author 11
 * @date 2026/5/26
 */
public final class SimPbConverter {

    private SimPbConverter() {
    }

    /**
     * GuestData + destinations → GuestInfo
     */
    public static GuestInfo toGuestInfo(GuestData guestData, List<DestinationInfo> destinations, VisitorQuestCfg visitorQuestCfg) {
        GuestInfo info = new GuestInfo();
        info.id = visitorQuestCfg.getResource() > 0 ? visitorQuestCfg.getResource() : guestData.getId();
        info.destinations = destinations;
        return info;
    }

    /**
     * PurchasedGuestData → GuestInfo (uid + 目的地; 用于生成下发 / 重连补发, 目的地已含预生成奖励)
     */
    public static GuestInfo toGuestInfo(PurchasedGuestData data, VisitorQuestCfg visitorQuestCfg) {
        GuestInfo info = new GuestInfo();
        info.id = visitorQuestCfg.getResource() > 0 ? visitorQuestCfg.getResource() : data.getGuestId();
        info.uid = data.getUid();
        if (data.getDestinations() != null && !data.getDestinations().isEmpty()) {
            info.destinations = data.getDestinations().values().stream().toList();
        }
        return info;
    }

    public static BuildingInfo toBuildingInfo(SimPlayerContext ctx, SimConfigCacheService simConfigCacheService,
                                              BuildingData buildingData, BuildingUpgradeTableCfg currentLevelCfg, int unlockGameId, long now) {
        BuildingInfo info = new BuildingInfo();
        info.id = buildingData.getId();
        info.level = buildingData.getLevel();
        info.cdEndTime = buildingData.getCdEndTime();
        info.progress = buildingData.getProgress();
        info.watchAdCount = buildingData.getAdClearCount();
        info.cdZero = (buildingData.getCdEndTime() > 0 && buildingData.getCdEndTime() <= now);

        info.skillConditionPass = true;
        if (unlockGameId > 0) {
            //检查技能等级
            SimSkillsData skillData = ctx.getSkillData(unlockGameId);
            if (skillData == null || skillData.allLevel() < currentLevelCfg.getSkillLevel()) {
                info.skillConditionPass = false;
            }
        }

        //交互次数
        info.interactCount = buildingData.getReceptCount();
        int operateBuildId = simConfigCacheService.getCasinoManageBuildId(ctx.getCurrentCasino().getCasinoId(), ServerBuildingType.OPERATIONS);
        if (operateBuildId > 0) {
            //交互次数
            if (buildingData.getId() == operateBuildId) {
                CasinoStatsSheetCfg casinoCfg = simConfigCacheService.getCasinoStatsSheetCfg(
                        ctx.getCurrentCasino().getCasinoId(), ctx.getCurrentCasino().getCasinoLevel());
                //运营看板每分钟获客量
                info.interactCount = Math.toIntExact(CommonUtil.getContext()
                        .getBean(SimOperationDashboardService.class).customerAcquisitionPerMinute(casinoCfg, ctx.getCurrentCasino(), System.currentTimeMillis()));
            }
        } else {
            int marketBuildId = simConfigCacheService.getCasinoManageBuildId(ctx.getCurrentCasino().getCasinoId(), ServerBuildingType.MARKETING);
            if (marketBuildId > 0) {
                GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Global.GUEST_AWARENESS_MAX);
                List<KVInfo> tmpList = new ArrayList<>();
                int sum = 0;
                for (Map.Entry<Integer, List<VisitorQuestCfg>> en : simConfigCacheService.getVisitorQuestCfgMap().entrySet()) {
                    KVInfo kvInfo = new KVInfo();
                    kvInfo.key = en.getKey();
                    VisitorQuestCfg visitorQuestCfg = en.getValue().stream().findFirst().get();
                    kvInfo.value = (int) ((double) ctx.getCurrentCasino().getAwareness() / globalConfigCfg.getIntValue() * visitorQuestCfg.getAwareness() + visitorQuestCfg.getBaseWeight());
                    sum += kvInfo.value;
                    tmpList.add(kvInfo);
                }

                if (sum > 0) {
                    System.out.println(sum);
                    info.guestQualityList = new ArrayList<>();
                    for (KVInfo kv : tmpList) {
                        KVInfo kvInfo = new KVInfo();
                        kvInfo.key = kv.key;
                        kvInfo.value = (int) ((double) kv.value / sum * 100);
                        info.guestQualityList.add(kvInfo);
                    }
                }
            }
        }

        info.bonusInfos = new ArrayList<>();
        CommonUtil.getContext().getBean(SimBuildingService.class)
                .computeDashboardBuildingValues(ctx, buildingData, info.bonusInfos);
        return info;
    }

    public static EmployeeInfo toEmployeeInfo(SimEmployeeData data) {
        EmployeeInfo info = new EmployeeInfo();
        info.id = data.getEmployeeId();
        info.level = data.getLevel();
        info.star = data.getStar();
        return info;
    }

    public static KVInfo toManageEmpInfo(int buildingType, int id) {
        KVInfo info = new KVInfo();
        info.key = buildingType;
        info.value = id;
        return info;
    }

    /**
     * SimSkillsData → GameSkills
     */
    public static GameSkills toGameSkills(SimSkillsData data) {
        GameSkills gs = new GameSkills();
        gs.gameType = data.getGameType();

        if (data.getSkillsMap() != null && !data.getSkillsMap().isEmpty()) {
            gs.skillInfos = new ArrayList<>();
            for (Map.Entry<Integer, SkillDetailData> en : data.getSkillsMap().entrySet()) {
                SkillInfo info = new SkillInfo();
                info.propId = en.getKey();
                info.level = en.getValue().getLevel();
                info.addOutPut = en.getValue().getAddOutPut();
                gs.skillInfos.add(info);
            }
        }
        return gs;
    }

    /**
     * 场景建筑列表 -> 协议结构 (无建筑返回 null)
     */
    public static List<BuildingInfo> toBuildingInfos(SimPlayerContext ctx, SimConfigCacheService simConfigCacheService) {
        if (ctx.getCurrentCasino() == null || ctx.getCurrentCasino().getBuildingData() == null || ctx.getCurrentCasino().getBuildingData().isEmpty()) {
            return null;
        }
        List<BuildingInfo> list = new ArrayList<>(ctx.getCurrentCasino().getBuildingData().size());
        long now = System.currentTimeMillis();


        for (BuildingData b : ctx.getCurrentCasino().getBuildingData().values()) {
            BuildingAreaTableCfg buildingAreaTableCfg = GameDataManager.getBuildingAreaTableCfg(b.getId());
            BuildingUpgradeTableCfg buildingUpgradeCfg = simConfigCacheService.getBuildingUpgradeCfg(b.getId(), b.getLevel());
            list.add(SimPbConverter.toBuildingInfo(ctx, simConfigCacheService, b, buildingUpgradeCfg, buildingAreaTableCfg.getUnlockGameId(), now));
        }
        return list;
    }

    /**
     * 场景主管列表 -> 协议结构 (无主管返回 null)
     */
    public static List<KVInfo> toManagerInfos(SimCasinoData casino) {
        if (casino == null || casino.getManagerEmployMap() == null || casino.getManagerEmployMap().isEmpty()) {
            return null;
        }
        List<KVInfo> list = new ArrayList<>(casino.getManagerEmployMap().size());
        for (Map.Entry<Integer, Integer> en : casino.getManagerEmployMap().entrySet()) {
            list.add(SimPbConverter.toManageEmpInfo(en.getKey(), en.getValue()));
        }
        return list;
    }
}
