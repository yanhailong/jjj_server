package com.jjg.game.sim.service;

import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.CasinoData;
import com.jjg.game.sim.data.Destination;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.struct.BuildingInfo;
import com.jjg.game.sim.pb.struct.GuestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 玩家重连处理
 *
 * @author 11
 * @date 2026/5/26
 */
@Service
public class SimReconnectService {
    private static final Logger log = LoggerFactory.getLogger(SimReconnectService.class);

    @Autowired
    private SimGuestService guestService;
    @Autowired
    private SimRewardService rewardService;

    /**
     * 重连处理:
     * - 长时掉线 → 清除所有在场游客, 返回空列表
     * - 短时掉线:
     * - 在路上 (currentBuildingId==0) → 补发剩余 destinations 奖励, 下线
     * - 在建筑里 → 按 BuildingData(接待+排队) 聚合 BuildingInfo 下发
     */
    public List<BuildingInfo> handleReconnect(SimPlayerContext ctx) {
        long offlineTime = ctx.getPlayerGameData().getLastOfflineTime();
        if (offlineTime <= 0) {
            return Collections.emptyList();
        }

        CasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();
        boolean longOffline = (now - offlineTime) >= SimConstant.Common.DISCONNECT_LONG_THRESHOLD_MS;
        Map<Integer, GuestData> guestMap = casino.getGuestMap();
        if (guestMap == null || guestMap.isEmpty()) {
            return Collections.emptyList();
        }

        //长时掉线
        if (longOffline) {
            for (GuestData guest : guestMap.values()) {
                if (!guest.isOnline()) {
                    continue;
                }
                settleAndOffLine(ctx, casino, guest);
            }
            return Collections.emptyList();
        }

        //短时掉线 - 第1步: 在路上的在线游客直接结算剩余 destinations 后下线
        for (GuestData guest : guestMap.values()) {
            if (!guest.isOnline() || guest.getCurrentBuildingId() != 0) {
                continue;
            }
            settleAndOffLine(ctx, casino, guest);
        }

        //短时掉线 - 第2步: 按建筑分组下发当前接待+排队中的在线游客
        Map<Integer, BuildingData> buildingMap = casino.getBuildingData();
        if (buildingMap == null || buildingMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<BuildingInfo> result = new ArrayList<>();
        for (Map.Entry<Integer, BuildingData> en : buildingMap.entrySet()) {
            BuildingData bd = en.getValue();
            if (bd == null) {
                continue;
            }

            Set<Integer> ids = new HashSet<>();
            if (bd.getGuestId() != null) {
                ids.addAll(bd.getGuestId());
            }
            if (bd.getWaitGuestId() != null) {
                ids.addAll(bd.getWaitGuestId());
            }
            if (ids.isEmpty()) {
                continue;
            }

            List<GuestInfo> guestInfos = new ArrayList<>();
            for (Integer gid : ids) {
                GuestData g = guestMap.get(gid);
                if (g != null && g.isOnline()) {
                    guestInfos.add(SimPbConverter.toGuestInfo(g));
                }
            }
            if (guestInfos.isEmpty()) {
                continue;
            }

            BuildingInfo bi = new BuildingInfo();
            bi.id = en.getKey();
            bi.guestInfoLst = guestInfos;
            result.add(bi);
        }
        return result;
    }

    /**
     * 重连时"在路上"游客的结算: 对每个未完成 destination 补发奖励, 再清理建筑数据并 offLine
     */
    private void settleAndOffLine(SimPlayerContext ctx, CasinoData casino, GuestData guest) {
        int pending = 0;
        if (guest.getDestinations() != null) {
            for (Destination dest : guest.getDestinations()) {
                if (!dest.isDone()) {
                    dest.setDone(true);
                    rewardService.grantReward(ctx, guest);
                    pending++;
                }
            }
        }
        guestService.removeGuestFromAllBuildings(casino, guest);
        guest.offLine();
        ctx.markCasinoDirty(casino.getCasinoId());
        log.info("重连结算路上游客并下线 playerId={},guestId={},补发次数={}", ctx.playerId(), guest.getId(), pending);
    }
}
