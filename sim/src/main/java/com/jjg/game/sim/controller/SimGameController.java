package com.jjg.game.sim.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.BuildingData;
import com.jjg.game.sim.data.Destination;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimPlayerGameData;
import com.jjg.game.sim.pb.res.NotifyGenerateGuest;
import com.jjg.game.sim.pb.strcut.BuildingInfo;
import com.jjg.game.sim.pb.strcut.GuestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 模拟经营游戏的控制器
 * 处理单个玩家的逻辑
 *
 * @author 11
 * @date 2026/5/21
 */
public class SimGameController {
    private static final Logger log = LoggerFactory.getLogger(SimGameController.class);

    private PlayerController playerController;
    private SimPlayerGameData playerGameData;

    public PlayerController getPlayerController() {
        return playerController;
    }

    public void setPlayerController(PlayerController playerController) {
        this.playerController = playerController;
    }

    public SimPlayerGameData getPlayerGameData() {
        return playerGameData;
    }

    public void setPlayerGameData(SimPlayerGameData playerGameData) {
        this.playerGameData = playerGameData;
    }

    /**
     * 生成游客
     */
    public void generateGuestEvent(long now,
                                   Map<Integer, Map<Integer, VisitorLevelCfg>> levelMap,
                                   Map<Integer, Map<Integer, VisitorStarCfg>> starMap) {
        //新手引导未完成不生成
        if (!this.playerGameData.isGuide()) {
            return;
        }
        //赌场配置
        CasinoStatsSheetCfg casinoCfg = GameDataManager.getCasinoStatsSheetCfg(this.playerGameData.getCasinoData().getId());
        if (casinoCfg == null) {
            log.warn("生成游客失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={}", this.playerGameData.getPlayerId(), this.playerGameData.getCasinoData().getId());
            return;
        }
        //计算实际生成间隔(ms)
        long intervalMs = computeVisitIntervalMs(casinoCfg, this.playerGameData, now);
        if (this.playerGameData.getLastGenerateTime() != 0 && now - this.playerGameData.getLastGenerateTime() < intervalMs) {
            return;
        }

        //检查是否有解锁的游客
        if (this.playerGameData.getGuestMap() == null || this.playerGameData.getGuestMap().isEmpty()) {
            return;
        }

        //获取所有不在线的游客
        List<GuestData> offLineGuestList = new ArrayList<>();
        for (Map.Entry<Integer, GuestData> en : this.playerGameData.getGuestMap().entrySet()) {
            if (!en.getValue().isOnline()) {
                offLineGuestList.add(en.getValue());
            }
        }

        if (offLineGuestList.isEmpty()) {
            return;
        }

        //是否曝光
        boolean exposed = this.playerGameData.isExposed(now);

        GuestData existingGuest = offLineGuestList.get(RandomUtils.randomInt(offLineGuestList.size()));
        VisitorCfg visitorCfg = GameDataManager.getVisitorCfg(existingGuest.getId());

        int interactionCount = computeInteractionCount(visitorCfg.getBaseDwellTime(), casinoCfg.getProsperity(), getStarCfg(visitorCfg.getQuality(), existingGuest.getStar(), starMap));
        if (interactionCount <= 0) {
            this.playerGameData.setLastGenerateTime(now);
            log.info("生成游客但交互次数为 0, 跳过 playerId={},guestId={}", this.playerGameData.getPlayerId(), visitorCfg.getId());
            return;
        }

        List<Destination> destinations = planDestinations(this.playerGameData, visitorCfg, interactionCount, existingGuest.getId());
        if (destinations.isEmpty()) {
            this.playerGameData.setLastGenerateTime(now);
            log.warn("生成游客失败，目的地序列为空 playerId={},guestId={}", this.playerGameData.getPlayerId(), visitorCfg.getId());
            return;
        }

        //累加经验 / 更新 guestMap
        GuestData guest = guestVisit(this.playerGameData, visitorCfg, levelMap);
        if (guest == null) {
            log.warn("生成游客失败，该游客之前就在线 playerId={},guestCfg={}", this.playerGameData.getPlayerId(), visitorCfg.getId());
            return;
        }

        guest.setDestinations(destinations);
        guest.setCurrentBuildingId(0);

        this.playerGameData.setLastGenerateTime(now);

        //下发通知
        NotifyGenerateGuest notify = new NotifyGenerateGuest(Code.SUCCESS);
        notify.guests = Collections.singletonList(guest.toGuestInfo());
        this.playerController.send(notify);

        log.info("生成游客成功 playerId={},guestId={},exposed={},exp={},level={},destinations={}",
                this.playerGameData.getPlayerId(), visitorCfg.getId(), exposed, guest.getExp(), guest.getLevel(), JSON.toJSONString(guest.getDestinations()));
    }

    /**
     * 计算实际来访间隔 (ms)
     */
    private long computeVisitIntervalMs(CasinoStatsSheetCfg casinoCfg, SimPlayerGameData data, long now) {
        long base = (long) casinoCfg.getBaseVisitInterval() * 1000L;
        if (!data.isExposed(now)) {
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
    private GuestData guestVisit(SimPlayerGameData data, VisitorCfg visitorCfg, Map<Integer, Map<Integer, VisitorLevelCfg>> levelMap) {
        Map<Integer, GuestData> guestMap = data.getGuestMap();
        if (guestMap == null) {
            guestMap = new HashMap<>();
            data.setGuestMap(guestMap);
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
        guest.addExp(levelMap);
        guest.setOnline(true);
        return guest;
    }

    /**
     * 规划本次行程的目的地序列
     * - InteractionWeight 非空 → 随机模式: 每次按权重独立随机选建筑
     * - 否则 TargetArea 非空 → 固定模式: 按顺序取 targetArea[i % size], 交互次数超出列表长度时循环
     *
     * @param data
     * @param visitorCfg
     * @param interactionCount
     */
    private List<Destination> planDestinations(SimPlayerGameData data, VisitorCfg visitorCfg, int interactionCount, int guestId) {
        List<Destination> result = new ArrayList<>(interactionCount);

        //随机模式
        List<List<Integer>> interactionWeight = visitorCfg.getInteractionWeight();
        if (interactionWeight != null && !interactionWeight.isEmpty()) {
            for (int i = 0; i < interactionCount; i++) {
                Destination dest = pickRandomDestination(data, visitorCfg, guestId);
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
                Destination dest = tryPickBuildingDevice(data, buildingId, guestId);
                if (dest != null) {
                    result.add(dest);
                }
            }
            if (result.isEmpty()) {
                log.warn("固定模式，所有的建筑已满 playerId={},visitorQuestId={}", this.playerGameData.getPlayerId(), visitorCfg.getId());
            }
            return result;
        }

        return result;
    }


    /**
     * 尝试在指定建筑挑一个交互设备; 若建筑不存在或当前总占用 (接待+等待+预占) 已满返回 null
     * 选中后立刻把 guestId 写入该建筑的预占集合
     */
    private Destination tryPickBuildingDevice(SimPlayerGameData data, int buildingId, int guestId) {
        InteractionAreasTableCfg cfg = GameDataManager.getInteractionAreasTableCfg(buildingId);
        if (cfg == null) {
            return null;
        }
        if (cfg.getDeviceLimit() == null || cfg.getDeviceLimit().isEmpty()) {
            return null;
        }

        BuildingData bd = getOrCreateBuildingData(data, buildingId);
        int totalCapacity = cfg.getSeatingCapacity() + cfg.getQueueLimit();
        //游客已经在该建筑占位时, 不再算新占位 (同建筑多次交互复用同一槽位)
        int virtualOccupancy = bd.occupancy();
        if (!isGuestInBuilding(bd, guestId) && virtualOccupancy >= totalCapacity) {
            return null;
        }

        //随机一个设备 id
        int deviceId = cfg.getDeviceLimit().get(RandomUtils.randomInt(cfg.getDeviceLimit().size()));

        //预占
        bd.addReserve(guestId);
        return new Destination(buildingId, deviceId);
    }

    /**
     * 获取或新建该建筑的 BuildingData
     */
    private BuildingData getOrCreateBuildingData(SimPlayerGameData data, int buildingId) {
        Map<Integer, BuildingData> map = data.getBuildingData();
        if (map == null) {
            map = new HashMap<>();
            data.setBuildingData(map);
        }
        BuildingData bd = map.get(buildingId);
        if (bd == null) {
            bd = new BuildingData();
            bd.setId(buildingId);
            map.put(buildingId, bd);
        }
        return bd;
    }

    private boolean isGuestInBuilding(BuildingData bd, int guestId) {
        if (bd.getGuestId() != null && bd.getGuestId().contains(guestId)) return true;
        if (bd.getWaitGuestId() != null && bd.getWaitGuestId().contains(guestId)) return true;
        if (bd.getReserveGuestId() != null && bd.getReserveGuestId().contains(guestId)) return true;
        return false;
    }

    /**
     * 按 InteractionWeight 加权随机一个建筑, 满员则换一个, MAX_DEST_PICK_RETRY 次内未找到返回 null
     */
    private Destination pickRandomDestination(SimPlayerGameData data, VisitorCfg visitorCfg, int guestId) {
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
            Destination dest = tryPickBuildingDevice(data, buildingId, guestId);
            if (dest != null) {
                return dest;
            }
            //该建筑满员或不存在, 从候选池剔除后重试
            int bid = buildingId;
            pool.removeIf(pair -> pair.size() >= 2 && pair.get(0) == bid);
        }
        return null;
    }

    /**
     * 按 [buildingId, weight] 列表加权随机
     */
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

    /**
     * 重连处理:
     * - 长时掉线 → 清除所有在场游客, 返回空列表
     * - 短时掉线:
     *     - 在路上 (currentBuildingId==0) → 补发剩余 destinations 奖励, 下线
     *     - 在建筑里 → 按 BuildingData(接待+排队) 聚合 BuildingInfo 下发
     */
    public List<BuildingInfo> handleReconnect(Map<Integer, Map<Integer, VisitorLevelCfg>> levelMap,
                                              Map<Integer, Map<Integer, VisitorStarCfg>> starMap) {
        long offlineTime = this.playerGameData.getLastOfflineTime();
        if (offlineTime <= 0) {
            return Collections.emptyList();
        }

        //获取所有解锁的游客
        long now = System.currentTimeMillis();
        boolean longOffline = (now - offlineTime) >= SimConstant.Common.DISCONNECT_LONG_THRESHOLD_MS;
        Map<Integer, GuestData> guestMap = this.playerGameData.getGuestMap();
        if (guestMap == null || guestMap.isEmpty()) {
            return Collections.emptyList();
        }

        //长时掉线
        if (longOffline) {
            for (GuestData guest : guestMap.values()) {
                if (!guest.isOnline()) {
                    continue;
                }
                settleAndOffLine(guest, levelMap, starMap);
            }
            return Collections.emptyList();
        }

        //短时掉线 - 第1步: 在路上的在线游客直接结算剩余 destinations 后下线
        for (GuestData guest : guestMap.values()) {
            if (!guest.isOnline() || guest.getCurrentBuildingId() != 0) {
                continue;
            }
            settleAndOffLine(guest, levelMap, starMap);
        }

        //短时掉线 - 第2步: 按建筑分组下发当前接待+排队中的在线游客
        Map<Integer, BuildingData> buildingMap = this.playerGameData.getBuildingData();
        if (buildingMap == null || buildingMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<BuildingInfo> result = new ArrayList<>();
        for (Map.Entry<Integer, BuildingData> en : buildingMap.entrySet()) {
            BuildingData bd = en.getValue();
            if (bd == null) {
                continue;
            }

            //获取当前建筑所有的游客
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
                    guestInfos.add(g.toGuestInfo());
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
     * 同步游客的目的地
     *
     * @param guestId
     * @param buildingId
     * @return
     */
    public int guestLocation(int guestId, int buildingId, boolean enter,
                             Map<Integer, Map<Integer, VisitorLevelCfg>> visitorLevelCfgMap,
                             Map<Integer, Map<Integer, VisitorStarCfg>> visitorStarCfgMap) {
        GuestData guest = this.playerGameData.findGuestData(guestId);
        if (guest == null || !guest.isOnline()) {
            log.warn("同步游客位置: GuestData 不存在或已离场 playerId={},guestId={}", playerController.playerId(), guestId);
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
                            grantReward(guest, visitorLevelCfgMap, visitorStarCfgMap);
                        }
                    }
                }

                if (flag) {
                    moveGuestIntoBuilding(buildingId, guestId);
                    log.info("游客到达建筑 playerId={},guestId={},building={}", playerController.playerId(), guestId, buildingId);
                } else {
                    log.info("游客同步位置时未找到该建筑 playerId={},guestId={},building={}", playerController.playerId(), guestId, buildingId);
                    return Code.NOT_FOUND;
                }
            } else {
                //离开建筑: 按传入 buildingId 从 BuildingData 接待/排队中移除
                removeGuestFromBuilding(buildingId, guestId);
                guest.setCurrentBuildingId(0);
                if (guest.isAllDestinationsDone()) {
                    log.info("游客目的地已完成 playerId={},guestId={}", playerController.playerId(), guestId);
                } else {
                    log.info("游客离开建筑前往下一站 playerId={},guestId={}", playerController.playerId(), guestId);
                }
            }
        } else {
            //离场: 清理该游客在所有建筑中的痕迹, 再 offLine
            removeGuestFromAllBuildings(guest);
            guest.offLine();
            log.info("游客离场 playerId={},guestId={}", playerController.playerId(), guestId);
        }
        return Code.SUCCESS;
    }

    /**
     * 游客到达建筑: 从 reserve 释放, 加入 seating (满则进 wait, 都满走兜底加 seating 并打 warn)
     */
    private void moveGuestIntoBuilding(int buildingId, int guestId) {
        Map<Integer, BuildingData> map = this.playerGameData.getBuildingData();
        if (map == null) {
            return;
        }
        BuildingData bd = map.get(buildingId);
        if (bd == null) {
            return;
        }
        bd.removeReserve(guestId);

        InteractionAreasTableCfg cfg = GameDataManager.getInteractionAreasTableCfg(buildingId);
        int seatingCap = cfg != null ? cfg.getSeatingCapacity() : Integer.MAX_VALUE;
        int queueCap = cfg != null ? cfg.getQueueLimit() : 0;

        if (bd.seatingSize() < seatingCap) {
            bd.addSeating(guestId);
        } else if (bd.waitSize() < queueCap) {
            bd.addWait(guestId);
        } else {
            //预占已扣, 但实际接待+排队都满 (理论上不应发生), 兜底加到 seating 并告警
            bd.addSeating(guestId);
            log.warn("游客到达时建筑已满, 强行入座 playerId={},guestId={},buildingId={}", this.playerGameData.getPlayerId(), guestId, buildingId);
        }
    }

    /**
     * 游客离开建筑: 从该建筑的接待与排队集合中移除
     */
    private void removeGuestFromBuilding(int buildingId, int guestId) {
        Map<Integer, BuildingData> map = this.playerGameData.getBuildingData();
        if (map == null) {
            return;
        }
        BuildingData bd = map.get(buildingId);
        if (bd == null) {
            return;
        }
        bd.removeSeating(guestId);
        bd.removeWait(guestId);
    }

    /**
     * 重连时"在路上"游客的结算: 对每个未完成 destination 补发奖励, 再清理建筑数据并 offLine
     */
    private void settleAndOffLine(GuestData guest,
                                  Map<Integer, Map<Integer, VisitorLevelCfg>> levelMap,
                                  Map<Integer, Map<Integer, VisitorStarCfg>> starMap) {
        int pending = 0;
        if (guest.getDestinations() != null) {
            for (Destination dest : guest.getDestinations()) {
                if (!dest.isDone()) {
                    dest.setDone(true);
                    grantReward(guest, levelMap, starMap);
                    pending++;
                }
            }
        }
        removeGuestFromAllBuildings(guest);
        guest.offLine();
        log.info("重连结算路上游客并下线 playerId={},guestId={},补发次数={}", this.playerGameData.getPlayerId(), guest.getId(), pending);
    }

    /**
     * 清理该游客在所有建筑中的痕迹 (用于离场 / 长时掉线)
     * 1) 当前所在建筑的接待/排队中移除
     * 2) destinations 涉及的所有建筑的预占中移除
     */
    private void removeGuestFromAllBuildings(GuestData guest) {
        Map<Integer, BuildingData> map = this.playerGameData.getBuildingData();
        if (map == null || map.isEmpty()) {
            return;
        }
        int gid = guest.getId();
        if (guest.getCurrentBuildingId() > 0) {
            BuildingData bd = map.get(guest.getCurrentBuildingId());
            if (bd != null) {
                bd.removeSeating(gid);
                bd.removeWait(gid);
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


    /**
     * 发放本次交互产出 (按 Reward 加权随机一行, 叠加 VisitorLevel 加成)
     */
    private void grantReward(GuestData guest, Map<Integer, Map<Integer, VisitorLevelCfg>> visitorLevelCfgMap, Map<Integer, Map<Integer, VisitorStarCfg>> visitorStarCfgMap) {
        //星级配置，概率奖励
        VisitorStarCfg visitorStarCfg = getStarCfg(guest.getId(), guest.getStar(), visitorStarCfgMap);
        if (visitorStarCfg != null && visitorStarCfg.getBonusRate() != null && !visitorStarCfg.getBonusRate().isEmpty()) {
            List<List<Integer>> rewardList = visitorStarCfg.getBonusRate();
            if (rewardList != null && !rewardList.isEmpty()) {
                WeightRandom<List<Integer>> random = WeightRandom.create();
                for (List<Integer> row : rewardList) {
                    if (row != null && row.size() >= 3 && row.get(0) > 0) {
                        random.add(row, row.get(0));
                    }
                }
                List<Integer> picked = random.next();
                if (picked != null) {
                    log.info("交互时随机奖励 playerId={},visitorStarCfgId={},picked={}", playerController.playerId(), visitorStarCfg.getId(), picked);
                }
            }
        }

        //等级配置,固定奖励
        VisitorLevelCfg visitorLevelCfg = getLevelCfg(guest.getId(), guest.getLevel(), visitorLevelCfgMap);
        if (visitorLevelCfg != null && visitorLevelCfg.getReward() != null && !visitorLevelCfg.getReward().isEmpty()) {
            log.info("交互时固定奖励 playerId={},levelCfgId={},reward={}", playerController.playerId(), visitorLevelCfg.getId(), visitorLevelCfg.getReward());
        }
    }

    public int unlockGuest(int guestId) {
        GuestData guest = this.playerGameData.findGuestData(guestId);
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
            this.playerGameData.addGuest(guest);
        }

        log.info("解锁游客成功 guestId={},online={}", guestId, guest.isOnline());
        return Code.SUCCESS;
    }


    /**
     * 获取游客星级配置
     *
     * @param guestId
     * @param star
     * @param starMap
     * @return
     */
    public VisitorStarCfg getStarCfg(int guestId, int star, Map<Integer, Map<Integer, VisitorStarCfg>> starMap) {
        if (starMap == null || starMap.isEmpty()) {
            return null;
        }
        Map<Integer, VisitorStarCfg> tmpMap = starMap.get(guestId);
        if (tmpMap == null || tmpMap.isEmpty()) {
            return null;
        }
        return tmpMap.get(star);
    }

    /**
     * 获取等级配置
     *
     * @param guestId
     * @param level
     * @param levelMap
     * @return
     */
    public VisitorLevelCfg getLevelCfg(int guestId, int level, Map<Integer, Map<Integer, VisitorLevelCfg>> levelMap) {
        if (levelMap == null || levelMap.isEmpty()) {
            return null;
        }
        Map<Integer, VisitorLevelCfg> tmpMap = levelMap.get(guestId);
        if (tmpMap == null || tmpMap.isEmpty()) {
            return null;
        }
        return tmpMap.get(level);
    }

    public void printGuest() {
        Map<Integer, GuestData> guestMap = this.playerGameData.getGuestMap();
        if (guestMap == null || guestMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, GuestData> en : guestMap.entrySet()) {
            System.out.println(JSONObject.toJSONString(en.getValue()));
        }
    }
    public void printBuilding() {
        Map<Integer, BuildingData> guestMap = this.playerGameData.getBuildingData();
        if (guestMap == null || guestMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, BuildingData> en : guestMap.entrySet()) {
            System.out.println(JSONObject.toJSONString(en.getValue()));
        }
    }
}
