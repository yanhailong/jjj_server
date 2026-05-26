package com.jjg.game.sim.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.Code;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sampledata.bean.InteractionAreasTableCfg;
import com.jjg.game.sampledata.bean.VisitorCfg;
import com.jjg.game.sampledata.bean.VisitorStarCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.CasinoData;
import com.jjg.game.sim.data.Destination;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.NotifyGenerateGuest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 游客生成、目的地规划、位置同步、解锁
 *
 * @author 11
 * @date 2026/5/26
 */
@Service
public class SimGuestService implements SimPlayerTickHandler {
    private static final Logger log = LoggerFactory.getLogger(SimGuestService.class);

    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private SimRewardService rewardService;

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        generateGuestEvent(ctx, now);
    }

    @Override
    public int order() {
        return 10;
    }

    // ---------------------------------------------------------------------
    // 游客生成
    // ---------------------------------------------------------------------

    /**
     * 生成游客 (定时器入口)
     */
    public void generateGuestEvent(SimPlayerContext ctx, long now) {
        //新手引导未完成不生成
        if (!ctx.getPlayerGameData().isGuide()) {
            return;
        }
        //当前赌场
        CasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("生成游客失败，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getPlayerGameData().getCurrentCasinoId());
            return;
        }
        //赌场配置
        CasinoStatsSheetCfg casinoCfg = GameDataManager.getCasinoStatsSheetCfg(casino.getStatsId());
        if (casinoCfg == null) {
            log.warn("生成游客失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={}", ctx.playerId(), casino.getStatsId());
            return;
        }
        //计算实际生成间隔(ms)
        long intervalMs = computeVisitIntervalMs(casinoCfg, casino, now);
        if (casino.getLastGenerateTime() != 0 && now - casino.getLastGenerateTime() < intervalMs) {
            return;
        }

        //检查是否有解锁的游客
        if (casino.getGuestMap() == null || casino.getGuestMap().isEmpty()) {
            return;
        }

        //获取所有不在线的游客
        List<GuestData> offLineGuestList = new ArrayList<>();
        for (Map.Entry<Integer, GuestData> en : casino.getGuestMap().entrySet()) {
            if (!en.getValue().isOnline()) {
                offLineGuestList.add(en.getValue());
            }
        }
        if (offLineGuestList.isEmpty()) {
            return;
        }

        //是否曝光
        boolean exposed = casino.getExposureEndTime() > now;

        GuestData existingGuest = offLineGuestList.get(RandomUtils.randomInt(offLineGuestList.size()));
        VisitorCfg visitorCfg = GameDataManager.getVisitorCfg(existingGuest.getId());

        Map<Integer, Map<Integer, VisitorStarCfg>> starMap = configCache.getVisitorStarCfgMap();
        int interactionCount = computeInteractionCount(visitorCfg.getBaseDwellTime(), casinoCfg.getProsperity(),
                rewardService.getStarCfg(visitorCfg.getQuality(), existingGuest.getStar(), starMap));
        if (interactionCount <= 0) {
            casino.setLastGenerateTime(now);
            log.info("生成游客但交互次数为 0, 跳过 playerId={},guestId={}", ctx.playerId(), visitorCfg.getId());
            return;
        }

        List<Destination> destinations = planDestinations(casino, visitorCfg, interactionCount, existingGuest.getId());
        if (destinations.isEmpty()) {
            casino.setLastGenerateTime(now);
            log.warn("生成游客失败，目的地序列为空 playerId={},guestId={}", ctx.playerId(), visitorCfg.getId());
            return;
        }

        //累加经验 / 更新 guestMap
        GuestData guest = guestVisit(casino, visitorCfg);
        if (guest == null) {
            log.warn("生成游客失败，该游客之前就在线 playerId={},guestCfg={}", ctx.playerId(), visitorCfg.getId());
            return;
        }

        guest.setDestinations(destinations);
        guest.setCurrentBuildingId(0);

        casino.setLastGenerateTime(now);

        //下发通知
        NotifyGenerateGuest notify = new NotifyGenerateGuest(Code.SUCCESS);
        notify.guests = Collections.singletonList(SimPbConverter.toGuestInfo(guest));
        ctx.send(notify);

        log.info("生成游客成功 playerId={},guestId={},exposed={},exp={},level={},destinations={}",
                ctx.playerId(), visitorCfg.getId(), exposed, guest.getExp(), guest.getLevel(), JSON.toJSONString(guest.getDestinations()));
    }

    /**
     * 计算实际来访间隔 (ms)
     */
    private long computeVisitIntervalMs(CasinoStatsSheetCfg casinoCfg, CasinoData casino, long now) {
        long base = (long) casinoCfg.getBaseVisitInterval() * 1000L;
        if (casino.getExposureEndTime() <= now) {
            return base;
        }
        int coefficient = casinoCfg.getVisitIntervalCoefficient();
        if (coefficient <= 0) {
            return base;
        }
        return base * coefficient / SimConstant.Common.EXPOSURE_COEFFICIENT_BASE;
    }

    /**
     * 计算本次来访的交互次数:
     * 1) BaseDwellTime 先按 VisitorStarCfg.Additioncoefficient (百分比) 加成
     * 2) floor(adjustedDwellTime / Prosperity) + 小数部分概率触发 +1
     */
    private int computeInteractionCount(int baseDwellTime, int prosperity, VisitorStarCfg starCfg) {
        if (prosperity <= 0) {
            return 0;
        }

        //星级有时长加成
        if (starCfg != null && starCfg.getAdditioncoefficient() > 0) {
            baseDwellTime = (int) ((long) baseDwellTime * starCfg.getAdditioncoefficient() / 100);
        }

        double ratio = (double) baseDwellTime / (double) prosperity;
        int floor = (int) Math.floor(ratio);
        double frac = ratio - floor;
        if (frac > 0 && RandomUtils.getRandomNumDou() < frac) {
            floor += 1;
        }
        return floor;
    }

    /**
     * 记录游客来访: 累加经验
     */
    private GuestData guestVisit(CasinoData casino, VisitorCfg visitorCfg) {
        Map<Integer, GuestData> guestMap = casino.getGuestMap();
        if (guestMap == null) {
            guestMap = new HashMap<>();
            casino.setGuestMap(guestMap);
        }

        GuestData guest = guestMap.get(visitorCfg.getId());
        if (guest == null) {
            guest = new GuestData();
            guest.setId(visitorCfg.getId());
            guest.setStar(1);
            guest.setLevel(1);
            guest.setExp(0);
            guestMap.put(visitorCfg.getId(), guest);
        } else {
            if (guest.isOnline()) {
                return null;
            }
        }
        guest.addExp(configCache.getVisitorLevelCfgMap());
        guest.setOnline(true);
        return guest;
    }

    /**
     * 规划本次行程的目的地序列
     * - InteractionWeight 非空 → 随机模式: 每次按权重独立随机选建筑
     * - 否则 TargetArea 非空 → 固定模式: 按顺序取 targetArea[i % size], 交互次数超出列表长度时循环
     */
    private List<Destination> planDestinations(CasinoData casino, VisitorCfg visitorCfg, int interactionCount, int guestId) {
        List<Destination> result = new ArrayList<>(interactionCount);

        //随机模式
        List<List<Integer>> interactionWeight = visitorCfg.getInteractionWeight();
        if (interactionWeight != null && !interactionWeight.isEmpty()) {
            for (int i = 0; i < interactionCount; i++) {
                Destination dest = pickRandomDestination(casino, visitorCfg, guestId);
                if (dest != null) {
                    result.add(dest);
                }
            }
            return result;
        }

        //固定模式: 按顺序循环 TargetArea
        List<Integer> targetArea = visitorCfg.getTargetArea();
        if (targetArea != null && !targetArea.isEmpty()) {
            int len = targetArea.size();
            for (int i = 0; i < interactionCount; i++) {
                int buildingId = targetArea.get(i % len);
                Destination dest = tryPickBuildingDevice(casino, buildingId, guestId);
                if (dest != null) {
                    result.add(dest);
                }
            }
            if (result.isEmpty()) {
                log.warn("固定模式，所有的建筑已满 visitorQuestId={}", visitorCfg.getId());
            }
            return result;
        }

        return result;
    }

    /**
     * 尝试在指定建筑挑一个交互设备; 若建筑不存在或当前总占用 (接待+等待+预占) 已满返回 null
     * 选中后立刻把 guestId 写入该建筑的预占集合
     */
    private Destination tryPickBuildingDevice(CasinoData casino, int buildingId, int guestId) {
        InteractionAreasTableCfg cfg = GameDataManager.getInteractionAreasTableCfg(buildingId);
        if (cfg == null) {
            return null;
        }
        if (cfg.getDeviceLimit() == null || cfg.getDeviceLimit().isEmpty()) {
            return null;
        }

        BuildingData bd = getOrCreateBuildingData(casino, buildingId);
        int totalCapacity = cfg.getSeatingCapacity() + cfg.getQueueLimit();
        //游客已经在该建筑占位时, 不再算新占位
        if (!bd.contains(guestId) && bd.occupancy() >= totalCapacity) {
            return null;
        }

        int deviceId = cfg.getDeviceLimit().get(RandomUtils.randomInt(cfg.getDeviceLimit().size()));
        bd.addReserve(guestId);
        return new Destination(buildingId, deviceId);
    }

    /**
     * 按 InteractionWeight 加权随机一个建筑, 满员则换一个
     */
    private Destination pickRandomDestination(CasinoData casino, VisitorCfg visitorCfg, int guestId) {
        List<List<Integer>> weightList = visitorCfg.getInteractionWeight();
        if (weightList == null || weightList.isEmpty()) {
            return null;
        }
        List<List<Integer>> pool = new ArrayList<>(weightList);
        for (int t = 0; t < SimConstant.Common.MAX_DEST_PICK_RETRY && !pool.isEmpty(); t++) {
            Integer buildingId = randomBuildingIdByWeight(pool);
            if (buildingId == null) {
                return null;
            }
            Destination dest = tryPickBuildingDevice(casino, buildingId, guestId);
            if (dest != null) {
                return dest;
            }
            int bid = buildingId;
            pool.removeIf(pair -> pair.size() >= 2 && pair.get(0) == bid);
        }
        return null;
    }

    private Integer randomBuildingIdByWeight(List<List<Integer>> pairs) {
        WeightRandom<Integer> random = WeightRandom.create();
        for (List<Integer> pair : pairs) {
            if (pair == null || pair.size() < 2) {
                continue;
            }
            int weight = pair.get(1);
            if (weight > 0) {
                random.add(pair.get(0), weight);
            }
        }
        return random.next();
    }

    private BuildingData getOrCreateBuildingData(CasinoData casino, int buildingId) {
        Map<Integer, BuildingData> map = casino.getBuildingData();
        if (map == null) {
            map = new HashMap<>();
            casino.setBuildingData(map);
        }
        BuildingData bd = map.get(buildingId);
        if (bd == null) {
            bd = new BuildingData();
            bd.setId(buildingId);
            map.put(buildingId, bd);
        }
        return bd;
    }

    // ---------------------------------------------------------------------
    // 同步位置 / 建筑出入
    // ---------------------------------------------------------------------

    /**
     * 同步游客的目的地
     */
    public int guestLocation(SimPlayerContext ctx, int guestId, int buildingId, boolean enter) {
        CasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("同步游客位置: 当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getPlayerGameData().getCurrentCasinoId());
            return Code.NOT_FOUND;
        }
        GuestData guest = casino.findGuestData(guestId);
        if (guest == null || !guest.isOnline()) {
            log.warn("同步游客位置: GuestData 不存在或已离场 playerId={},guestId={}", ctx.playerId(), guestId);
            return Code.NOT_FOUND;
        }

        if (buildingId > 0) {
            if (enter) {
                //到达建筑: 校验并标记 done, 更新 BuildingData (reserve → seating/wait), 发奖
                boolean flag = false;
                if (guest.getDestinations() != null && !guest.getDestinations().isEmpty()) {
                    for (Destination dest : guest.getDestinations()) {
                        if (dest.getBuildingId() == buildingId && !dest.isDone()) {
                            flag = true;
                            dest.setDone(true);
                            guest.setCurrentBuildingId(buildingId);
                            rewardService.grantReward(ctx, guest);
                        }
                    }
                }

                if (flag) {
                    moveGuestIntoBuilding(casino, buildingId, guestId);
                    log.info("游客到达建筑 playerId={},guestId={},building={}", ctx.playerId(), guestId, buildingId);
                } else {
                    log.info("游客同步位置时未找到该建筑 playerId={},guestId={},building={}", ctx.playerId(), guestId, buildingId);
                    return Code.NOT_FOUND;
                }
            } else {
                //离开建筑
                removeGuestFromBuilding(casino, buildingId, guestId);
                guest.setCurrentBuildingId(0);
                if (guest.isAllDestinationsDone()) {
                    log.info("游客目的地已完成 playerId={},guestId={}", ctx.playerId(), guestId);
                } else {
                    log.info("游客离开建筑前往下一站 playerId={},guestId={}", ctx.playerId(), guestId);
                }
            }
        } else {
            //离场
            removeGuestFromAllBuildings(casino, guest);
            guest.offLine();
            log.info("游客离场 playerId={},guestId={}", ctx.playerId(), guestId);
        }
        return Code.SUCCESS;
    }

    /**
     * 游客到达建筑: 从 reserve 释放, 加入 seating (满则进 wait, 都满兜底加 seating 并打 warn)
     */
    private void moveGuestIntoBuilding(CasinoData casino, int buildingId, int guestId) {
        Map<Integer, BuildingData> map = casino.getBuildingData();
        if (map == null) {
            return;
        }
        BuildingData bd = map.get(buildingId);
        if (bd == null) {
            return;
        }
        InteractionAreasTableCfg cfg = GameDataManager.getInteractionAreasTableCfg(buildingId);
        int seatingCap = cfg != null ? cfg.getSeatingCapacity() : Integer.MAX_VALUE;
        int queueCap = cfg != null ? cfg.getQueueLimit() : 0;
        if (!bd.arriveSeating(guestId, seatingCap, queueCap)) {
            log.warn("游客到达时建筑已满, 强行入座 guestId={},buildingId={}", guestId, buildingId);
        }
    }

    /**
     * 游客离开建筑: 从该建筑的接待与排队集合中移除
     */
    private void removeGuestFromBuilding(CasinoData casino, int buildingId, int guestId) {
        Map<Integer, BuildingData> map = casino.getBuildingData();
        if (map == null) {
            return;
        }
        BuildingData bd = map.get(buildingId);
        if (bd == null) {
            return;
        }
        bd.leaveBuilding(guestId);
    }

    /**
     * 清理该游客在所有建筑中的痕迹 (用于离场 / 长时掉线)
     */
    public void removeGuestFromAllBuildings(CasinoData casino, GuestData guest) {
        Map<Integer, BuildingData> map = casino.getBuildingData();
        if (map == null || map.isEmpty()) {
            return;
        }
        int gid = guest.getId();
        if (guest.getCurrentBuildingId() > 0) {
            BuildingData bd = map.get(guest.getCurrentBuildingId());
            if (bd != null) {
                bd.leaveBuilding(gid);
            }
        }
        if (guest.getDestinations() != null && !guest.getDestinations().isEmpty()) {
            Set<Integer> unique = new HashSet<>();
            for (Destination d : guest.getDestinations()) {
                unique.add(d.getBuildingId());
            }
            for (int bid : unique) {
                BuildingData bd = map.get(bid);
                if (bd != null) {
                    bd.removeReserve(gid);
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // 解锁
    // ---------------------------------------------------------------------

    /**
     * 解锁游客
     */
    public int unlockGuest(SimPlayerContext ctx, int guestId) {
        CasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("解锁游客时，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getPlayerGameData().getCurrentCasinoId());
            return Code.NOT_FOUND;
        }
        GuestData guest = casino.findGuestData(guestId);
        if (guest == null) {
            VisitorCfg visitorCfg = GameDataManager.getVisitorCfg(guestId);
            if (visitorCfg == null) {
                log.warn("解锁游客时，未找到对应的配置 guestId={}", guestId);
                return Code.NOT_FOUND;
            }
            guest = new GuestData();
            guest.setId(guestId);
            guest.setLevel(1);
            guest.setStar(1);
            casino.addGuest(guest);
        }

        log.info("解锁游客成功 guestId={},online={}", guestId, guest.isOnline());
        return Code.SUCCESS;
    }

}
