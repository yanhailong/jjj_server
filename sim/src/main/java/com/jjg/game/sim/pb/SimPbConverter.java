package com.jjg.game.sim.pb;

import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimEmployeeData;
import com.jjg.game.sim.data.SimSkillsData;
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

    public static BuildingInfo toBuildingInfo(BuildingData buildingData) {
        BuildingInfo info = new BuildingInfo();
        info.id = buildingData.getId();
        info.level = buildingData.getLevel();
        info.cdEndTime = buildingData.getCdEndTime();
        info.progress = buildingData.getProgress();
        return info;
    }

    public static EmployeeInfo toEmployeeInfo(SimEmployeeData data){
        EmployeeInfo info = new EmployeeInfo();
        info.id = data.getEmployeeId();
        info.level = data.getLevel();
        info.star = data.getStar();
        return info;
    }

    public static KVInfo toManageEmpInfo(int buildingType,int id){
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
}
