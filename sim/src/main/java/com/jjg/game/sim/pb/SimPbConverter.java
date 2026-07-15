package com.jjg.game.sim.pb;

import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.pb.struct.*;

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
    public static GuestInfo toGuestInfo(GuestData guestData, List<DestinationInfo> destinations) {
        GuestInfo info = new GuestInfo();
        info.id = guestData.getId();
        info.destinations = destinations;
        return info;
    }

    /**
     * PurchasedGuestData → GuestInfo (uid + 目的地; 用于生成下发 / 重连补发, 目的地已含预生成奖励)
     */
    public static GuestInfo toGuestInfo(PurchasedGuestData data) {
        GuestInfo info = new GuestInfo();
        info.id = data.getGuestId();
        info.uid = data.getUid();
        if (data.getDestinations() != null && !data.getDestinations().isEmpty()) {
            info.destinations = data.getDestinations().values().stream().toList();
        }
        return info;
    }

    public static BuildingInfo toBuildingInfo(BuildingData buildingData, long now) {
        BuildingInfo info = new BuildingInfo();
        info.id = buildingData.getId();
        info.level = buildingData.getLevel();
        info.cdEndTime = buildingData.getCdEndTime();
        info.progress = buildingData.getProgress();
        info.watchAdCount = buildingData.getAdClearCount();
        info.cdZero = (buildingData.getCdEndTime() > 0 && buildingData.getCdEndTime() <= now);
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
            for (Map.Entry<Integer, Integer> en : data.getSkillsMap().entrySet()) {
                KVInfo kv = new KVInfo();
                kv.key = en.getKey();
                kv.value = en.getValue();
                gs.skillInfos.add(kv);
            }
        }
        return gs;
    }

    /**
     * 场景建筑列表 -> 协议结构 (无建筑返回 null)
     */
    public static List<BuildingInfo> toBuildingInfos(SimCasinoData casino) {
        if (casino == null || casino.getBuildingData() == null || casino.getBuildingData().isEmpty()) {
            return null;
        }
        List<BuildingInfo> list = new ArrayList<>(casino.getBuildingData().size());
        long now = System.currentTimeMillis();
        for (BuildingData b : casino.getBuildingData().values()) {
            list.add(SimPbConverter.toBuildingInfo(b, now));
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
