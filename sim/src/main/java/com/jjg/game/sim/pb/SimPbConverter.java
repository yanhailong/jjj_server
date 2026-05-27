package com.jjg.game.sim.pb;

import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimSkillsData;
import com.jjg.game.sim.pb.struct.DestinationInfo;
import com.jjg.game.sim.pb.struct.GameSkills;
import com.jjg.game.sim.pb.struct.GuestInfo;

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
     * SimSkillsData → GameSkills
     */
    public static GameSkills toGameSkills(SimSkillsData data) {
        GameSkills gs = new GameSkills();
        gs.gameType = data.getGameType();
        gs.stake = data.getStakeList();

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
