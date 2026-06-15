package com.jjg.game.sim.service;

import cn.hutool.core.lang.Snowflake;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.CasinoListCfg;
import com.jjg.game.sampledata.bean.CasinoStatsSheetCfg;
import com.jjg.game.sampledata.bean.VisitorQuestCfg;
import com.jjg.game.sampledata.bean.VisitorStarCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.NotifyGenerateGuest;
import com.jjg.game.sim.pb.res.ResAllGuest;
import com.jjg.game.sim.pb.res.ResGenPurchasedGuest;
import com.jjg.game.sim.pb.res.ResPurchasedGuestReward;
import com.jjg.game.sim.pb.struct.DestinationInfo;
import com.jjg.game.sim.pb.struct.GuestDetailInfo;
import com.jjg.game.sim.pb.struct.GuestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 游客生成、解锁
 *
 * @author 11
 * @date 2026/5/26
 */
@Service
public class SimGuestService implements SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SimGuestService.class);

    //购买游客 uid 生成器
    private static final Snowflake UID_GENERATOR = new Snowflake(1, 1);

    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private SimRewardService rewardService;
    @Autowired
    private SimPackService simPackService;

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        generateGuestEvent(ctx, now);
    }

    @Override
    public int order() {
        return 10;
    }

    /**
     * 生成游客 (定时器入口)
     */
    public void generateGuestEvent(SimPlayerContext ctx, long now) {
        //新手引导未完成不生成
        if (!ctx.getSimBaseData().isGuide()) {
            return;
        }
        //当前赌场
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("生成游客失败，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
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
            casino.setLastGenerateTime(now);
            return;
        }

        batchGenerateGuest(ctx, casinoCfg.getVisitorSpawnCount(), casinoCfg, now, null);
    }

    /**
     * 指定游客id批量生成
     *
     * @param ctx
     * @param guestId
     * @param num
     */
    public void batchGenerateSpecifyIdGuest(SimPlayerContext ctx, int guestId, int num) {
        //当前赌场
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("生成指定游客id失败，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
            return;
        }

        VisitorQuestCfg visitorQuestCfg = GameDataManager.getVisitorQuestCfg(guestId);
        if (visitorQuestCfg == null) {
            log.warn("生成指定游客id失败，获取 visitorQuestCfg 配置未找到 playerId={},casinoId={}", ctx.playerId(), casino.getStatsId());
            return;
        }

        //赌场配置
        CasinoStatsSheetCfg casinoCfg = GameDataManager.getCasinoStatsSheetCfg(casino.getStatsId());
        if (casinoCfg == null) {
            log.warn("生成指定游客id失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={}", ctx.playerId(), casino.getStatsId());
            return;
        }

        GuestData guestData = new GuestData();
        guestData.setId(guestId);
        guestData.setLevel(1);
        guestData.setStar(1);
        batchGenerateGuest(ctx, num, casinoCfg, System.currentTimeMillis(), guestData);
    }

    /**
     * 指定游客品质批量生成
     *
     * @param ctx
     * @param quality
     * @param num
     */
    public void batchGenerateSpecifyQualityGuest(SimPlayerContext ctx, int quality, int num) {
        //当前赌场
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("生成指定游客quality失败，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
            return;
        }
        //赌场配置
        CasinoStatsSheetCfg casinoCfg = GameDataManager.getCasinoStatsSheetCfg(casino.getStatsId());
        if (casinoCfg == null) {
            log.warn("生成指定游客quality失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={}", ctx.playerId(), casino.getStatsId());
            return;
        }

        List<VisitorQuestCfg> visitorQuestCfgList = configCache.getVisitorQuestCfgList(quality);
        if (visitorQuestCfgList == null || visitorQuestCfgList.isEmpty()) {
            log.warn("生成指定游客quality失败，获取 VisitorQuestCfg 配置未找到 playerId={},quality={}", ctx.playerId(), quality);
            return;
        }

        long now = System.currentTimeMillis();
        NotifyGenerateGuest notify = new NotifyGenerateGuest(Code.SUCCESS);
        notify.guests = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            VisitorQuestCfg visitorQuestCfg = RandomUtils.randomEle(visitorQuestCfgList);

            GuestData guestData = new GuestData();
            guestData.setId(visitorQuestCfg.getId());
            guestData.setLevel(1);
            guestData.setStar(1);

            //生成单个游客
            GuestInfo guestInfo = generateOneGuest(ctx, null, casinoCfg, now, guestData);
            if (guestInfo != null) {
                //将游客信息放入列表
                notify.guests.add(guestInfo);
            }
        }
        if (!notify.guests.isEmpty()) {
            ctx.send(notify);
        }
    }

    // ---------------------------------------------------------------------
    // 购买游客 (点击购买后立即生成, 不走定时逻辑; 只预生成目的地, 奖励延后领取)
    // ---------------------------------------------------------------------

    /**
     * 生成购买游客 (客户端点击购买后触发):
     * 只预生成目的地序列并分配唯一 uid, 不结算奖励
     *
     * @param ctx
     * @param guestId 购买的游客id
     */
    public void generatePurchasedGuest(SimPlayerContext ctx, int guestId) {
        ResGenPurchasedGuest res = new ResGenPurchasedGuest(Code.SUCCESS);

        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("生成购买游客失败，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
            res.code = Code.NOT_FOUND;
            ctx.send(res);
            return;
        }
        VisitorQuestCfg visitorQuestCfg = GameDataManager.getVisitorQuestCfg(guestId);
        if (visitorQuestCfg == null) {
            log.warn("生成购买游客失败，VisitorQuest 配置未找到 playerId={},guestId={}", ctx.playerId(), guestId);
            res.code = Code.NOT_FOUND;
            ctx.send(res);
            return;
        }
        CasinoStatsSheetCfg casinoCfg = GameDataManager.getCasinoStatsSheetCfg(casino.getStatsId());
        if (casinoCfg == null) {
            log.warn("生成购买游客失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={}", ctx.playerId(), casino.getStatsId());
            res.code = Code.FAIL;
            ctx.send(res);
            return;
        }

        //星级/等级取已解锁游客的快照, 未解锁则默认 1
        GuestData guest = new GuestData();
        guest.setId(guestId);
        guest.setStar(1);
        guest.setLevel(1);
        GuestData unlocked = casino.findGuestData(guestId);
        if (unlocked != null) {
            guest.setStar(unlocked.getStar());
            guest.setLevel(unlocked.getLevel());
        }

        //本次交互次数 (有奖励 + 无奖励)
        VisitorStarCfg starCfg = rewardService.getStarCfg(guestId, guest.getStar());
        int rewardedCount = computeInteractionCount(visitorQuestCfg.getServiceCapacity(), casinoCfg.getProsperity(), starCfg);
        int unrewardedCount = computeInteractionCount(visitorQuestCfg.getBaseServiceCapacity(), casinoCfg.getProsperity(), starCfg);
        if (rewardedCount + unrewardedCount <= 0) {
            log.info("生成购买游客但交互次数为 0, 跳过 playerId={},guestId={}", ctx.playerId(), guestId);
            res.code = Code.FAIL;
            ctx.send(res);
            return;
        }

        //复用定时生成的目的地规划,不生成奖励
        List<DestinationInfo> destinations = planDestinations(guest, visitorQuestCfg, casino, rewardedCount, unrewardedCount, false);
        if (destinations.isEmpty()) {
            log.warn("生成购买游客失败，目的地序列为空 playerId={},guestId={}", ctx.playerId(), guestId);
            res.code = Code.FAIL;
            ctx.send(res);
            return;
        }

        PurchasedGuestData data = new PurchasedGuestData();
        data.setUid(UID_GENERATOR.nextId());
        data.setGuestId(guestId);
        data.setStar(guest.getStar());
        data.setLevel(guest.getLevel());
        data.setDestinations(destinations);
        casino.addPurchasedGuest(data);

        log.info("生成购买游客成功 playerId={},guestId={},uid={},destSize={}", ctx.playerId(), guestId, data.getUid(), destinations.size());
        res.guest = SimPbConverter.toGuestInfo(data);
        ctx.send(res);
    }

    /**
     * 领取购买游客单个目的地的奖励 (客户端凭 uid + 目的地序号逐个发起):
     * 该目的地此时才结算奖励 (无奖励交互点返回空); 同一目的地重复领取幂等;
     * 全部目的地领取完毕后移除该购买游客。
     *
     * @param ctx
     * @param uid   购买游客唯一id
     * @param index 目的地序号 (destinations 下标)
     */
    public void claimPurchasedGuestReward(SimPlayerContext ctx, long uid, int index) {
        ResPurchasedGuestReward res = new ResPurchasedGuestReward(Code.SUCCESS);
        res.uid = uid;
        res.index = index;

        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("领取购买游客奖励失败，当前赌场数据不存在 playerId={},uid={}", ctx.playerId(), uid);
            res.code = Code.NOT_FOUND;
            ctx.send(res);
            return;
        }
        PurchasedGuestData data = casino.findPurchasedGuest(uid);
        if (data == null) {
            log.warn("领取购买游客奖励失败，未找到对应购买游客 playerId={},uid={}", ctx.playerId(), uid);
            res.code = Code.NOT_FOUND;
            ctx.send(res);
            return;
        }
        List<DestinationInfo> destinations = data.getDestinations();
        if (destinations == null || index < 0 || index >= destinations.size()) {
            log.warn("领取购买游客奖励失败，目的地序号越界 playerId={},uid={},index={}", ctx.playerId(), uid, index);
            res.code = Code.NOT_FOUND;
            ctx.send(res);
            return;
        }

        DestinationInfo dest = destinations.get(index);
        //首次领取才结算; 重复领取直接回已有奖励 (幂等)
        if (!dest.claimed) {
            if (dest.rewarded) {
                //用生成时的星级/等级快照结算奖励
                GuestData snapshot = new GuestData();
                snapshot.setId(data.getGuestId());
                snapshot.setStar(data.getStar());
                snapshot.setLevel(data.getLevel());
                rewardService.grantReward(snapshot, dest);

                if (dest.rewards != null && !dest.rewards.isEmpty()) {
                    Map<Integer, Long> rewardsMap = new HashMap<>();
                    for (ItemInfo itemInfo : dest.rewards) {
                        rewardsMap.merge(itemInfo.itemId, itemInfo.count, Long::sum);
                    }
                    //添加道具
                    simPackService.addItems(ctx, rewardsMap, AddType.SIM_GUEST_REWARDS, null, false);
                    //经营信息: 购买游客交互产出金币计入经营收益
                    casino.addBusinessIncome(rewardsMap.getOrDefault(ItemUtils.getGoldItemId(), 0L));
                }
            }
            dest.claimed = true;
            //经营信息: 接待游客人次 (购买游客每个目的地领取一次记一次交互)
            casino.addReceptionCount(1);
        }
        res.rewards = dest.rewards;

        //全部目的地领取完毕则移除
        if (isAllClaimed(destinations)) {
            casino.removePurchasedGuest(uid);
        }

        log.info("领取购买游客奖励成功 playerId={},guestId={},uid={},index={}", ctx.playerId(), data.getGuestId(), uid, index);
        ctx.send(res);
    }

    /**
     * 购买游客的所有目的地是否都已领取
     */
    private boolean isAllClaimed(List<DestinationInfo> destinations) {
        for (DestinationInfo d : destinations) {
            if (!d.claimed) {
                return false;
            }
        }
        return true;
    }

    /**
     * 批量生成游客
     *
     * @param ctx
     * @param num
     * @param casinoCfg
     * @param now
     * @param specifyGuest
     */
    public void batchGenerateGuest(SimPlayerContext ctx, int num, CasinoStatsSheetCfg casinoCfg, long now, GuestData specifyGuest) {
        NotifyGenerateGuest notify = new NotifyGenerateGuest(Code.SUCCESS);
        notify.guests = new ArrayList<>();

        WeightRandom<GuestData> guestDataWeightRandom = weightRandomGuest(ctx.getCurrentCasino());
        for (int i = 0; i < num; i++) {
            //生成单个游客
            GuestInfo guestInfo = generateOneGuest(ctx, guestDataWeightRandom, casinoCfg, now, specifyGuest);
            if (guestInfo != null) {
                //将游客信息放入列表
                notify.guests.add(guestInfo);
            }
        }

        if (!notify.guests.isEmpty()) {
            ctx.send(notify);
        }
    }

    /**
     * 生成单个游客，不推送消息
     *
     * @param ctx
     * @param guestDataWeightRandom
     * @param casinoCfg
     * @param now
     * @param specifyGuest
     * @return
     */
    private GuestInfo generateOneGuest(SimPlayerContext ctx, WeightRandom<GuestData> guestDataWeightRandom, CasinoStatsSheetCfg casinoCfg, long now, GuestData specifyGuest) {
        //加权随机选一个已解锁的游客
        GuestData guest = specifyGuest;
        if (guest == null) {
            if (guestDataWeightRandom == null) {
                return null;
            }
            guest = guestDataWeightRandom.next();
            if (guest == null) {
                ctx.getCurrentCasino().setLastGenerateTime(now);
                return null;
            }
        }
        VisitorQuestCfg visitorQuestCfg = GameDataManager.getVisitorQuestCfg(guest.getId());
        if (visitorQuestCfg == null) {
            ctx.getCurrentCasino().setLastGenerateTime(now);
            log.warn("生成游客失败，VisitorQuest 配置未找到 playerId={},guestId={}", ctx.playerId(), guest.getId());
            return null;
        }

        //本次交互次数 (有奖励 + 无奖励)
        VisitorStarCfg starCfg = rewardService.getStarCfg(guest.getId(), guest.getStar());
        int rewardedCount = computeInteractionCount(visitorQuestCfg.getServiceCapacity(), casinoCfg.getProsperity(), starCfg);
        int unrewardedCount = computeInteractionCount(visitorQuestCfg.getBaseServiceCapacity(), casinoCfg.getProsperity(), starCfg);
        if (rewardedCount + unrewardedCount <= 0) {
            ctx.getCurrentCasino().setLastGenerateTime(now);
            log.info("生成游客但交互次数为 0, 跳过 playerId={},guestId={}", ctx.playerId(), visitorQuestCfg.getId());
            return null;
        }

        //规划本次行程的目的地序列 (定时生成: 立即结算奖励)
        List<DestinationInfo> destinations = planDestinations(guest, visitorQuestCfg, ctx.getCurrentCasino(), rewardedCount, unrewardedCount, true);
        if (destinations.isEmpty()) {
            ctx.getCurrentCasino().setLastGenerateTime(now);
//            log.warn("生成游客失败，目的地序列为空 playerId={},guestId={}", ctx.playerId(), visitorQuestCfg.getId());
            return null;
        }

        Map<Integer, Long> rewardsMap = new HashMap<>();
        for (DestinationInfo info : destinations) {
            if (info.rewards == null || info.rewards.isEmpty()) {
                continue;
            }

            for (ItemInfo itemInfo : info.rewards) {
                rewardsMap.merge(itemInfo.itemId, itemInfo.count, Long::sum);
            }
        }
        if (!rewardsMap.isEmpty()) {
            //添加道具
            simPackService.addItems(ctx, rewardsMap, AddType.SIM_GUEST_REWARDS, null, false);
        }

        //经营信息: 接待游客人次(每个目的地一次交互) + 游客交互产出金币计入经营收益
        SimCasinoData statCasino = ctx.getCurrentCasino();
        statCasino.addReceptionCount(destinations.size());
        if (!rewardsMap.isEmpty()) {
            statCasino.addBusinessIncome(rewardsMap.getOrDefault(ItemUtils.getGoldItemId(), 0L));
        }

        //累加经验
        guest.addExp(configCache.getVisitorLevelCfgMap());

        ctx.getCurrentCasino().setLastGenerateTime(now);
        ctx.getCurrentCasino().recordGenerate(now, SimConstant.Common.CAPACITY_WINDOW_MS);
        return SimPbConverter.toGuestInfo(guest, destinations);
    }

    /**
     * 计算实际来访间隔 (ms)
     * 当赌场满员 (10 分钟内生成人数 >= 赌场容纳上限) 时降低刷新频率:
     * 实际间隔 = 基础间隔 * 当前总人数 / 上限
     */
    private long computeVisitIntervalMs(CasinoStatsSheetCfg casinoCfg, SimCasinoData casino, long now) {
        long base = (long) casinoCfg.getBaseVisitInterval() * 1000L;
        CasinoListCfg listCfg = GameDataManager.getCasinoListCfg(casino.getCasinoId());
        if (listCfg == null || listCfg.getCapacityNum() <= 0) {
            return base;
        }
        int capacity = listCfg.getCapacityNum();
        int recent = casino.countGenerateInWindow(now, SimConstant.Common.CAPACITY_WINDOW_MS);
        if (recent < capacity) {
            return base;
        }
        return base * recent / capacity;
    }

    /**
     * 计算单类交互次数 (有奖励 / 无奖励):
     * 1) 服务能力先按 VisitorStarCfg.Additioncoefficient (百分比) 加成
     * 2) floor(serviceCapacity / Prosperity) + 小数部分概率触发 +1
     */
    private int computeInteractionCount(int serviceCapacity, int prosperity, VisitorStarCfg starCfg) {
        if (serviceCapacity <= 0 || prosperity <= 0) {
            return 0;
        }
        int coefficient = SimConstant.Common.SERVICE_CAPACITY_COEFFICIENT_BASE;
        if (starCfg != null && starCfg.getAdditioncoefficient() > 0) {
            coefficient = starCfg.getAdditioncoefficient();
        }
        double effective = (double) serviceCapacity * coefficient / SimConstant.Common.SERVICE_CAPACITY_COEFFICIENT_BASE;
        double ratio = effective / (double) prosperity;
        int floor = (int) Math.floor(ratio);
        double frac = ratio - floor;
        if (frac > 0 && RandomUtils.getRandomNumDou() < frac) {
            floor += 1;
        }
        return floor;
    }

    /**
     * 按已解锁游客的刷新权重加权随机
     *
     * @param casino
     * @return
     */
    private WeightRandom<GuestData> weightRandomGuest(SimCasinoData casino) {
        WeightRandom<GuestData> random = WeightRandom.create();
        if (casino.getGuestMap() == null || casino.getGuestMap().isEmpty()) {
            return random;
        }
        for (GuestData g : casino.getGuestMap().values()) {
            VisitorQuestCfg cfg = GameDataManager.getVisitorQuestCfg(g.getId());
            if (cfg == null) {
                continue;
            }
            int weight = effectiveRefreshWeight(cfg);
            if (weight > 0) {
                random.add(g, weight);
            }
        }
        return random;
    }

    /**
     * 计算游客的有效刷新权重 (考虑知名度修正)
     */
    private int effectiveRefreshWeight(VisitorQuestCfg cfg) {
        int base = cfg.getBaseWeight();
        if (cfg.getAwareness() <= 0) {
            return base;
        }
        return (cfg.getAwareness() + 1) * base;
    }

    /**
     * 规划本次行程的目的地序列:
     * - 有奖励交互: 按 InteractionWeight 加权随机一个建筑, 标记 rewarded=true
     * - 无奖励交互: 按 TargetArea 顺序循环
     * - 两类穿插; 简单实现为先有奖励, 再无奖励
     * - 目标建筑未解锁/未配置则跳过该次交互
     *
     * @param rollReward true 立即结算奖励 (定时生成); false 只标记 rewarded 不结算 (购买游客延后领取)
     */
    private List<DestinationInfo> planDestinations(GuestData guest, VisitorQuestCfg cfg, SimCasinoData casino, int rewardedCount, int unrewardedCount, boolean rollReward) {
        int allCount = rewardedCount + unrewardedCount;
        if (allCount < 1) {
            return Collections.emptyList();
        }
        List<DestinationInfo> result = new ArrayList<>(allCount);

        //有奖励: InteractionWeight (Map<buildingId, weight>)
        Map<Integer, Integer> interactionWeight = cfg.getInteractionWeight();
        if (rewardedCount > 0 && interactionWeight != null && !interactionWeight.isEmpty()) {
            WeightRandom<Integer> random = WeightRandom.create();
            for (Map.Entry<Integer, Integer> en : interactionWeight.entrySet()) {
                if (en.getValue() == null || en.getValue() <= 0) {
                    continue;
                }
                if (!isBuildingUnlocked(en.getKey(), casino)) {
                    continue;
                }
                random.add(en.getKey(), en.getValue());
            }
            for (int i = 0; i < rewardedCount; i++) {
                Integer buildingId = random.next();
                if (buildingId == null) {
                    continue;
                }
                DestinationInfo dest = pickBuildingDevice(buildingId, casino);
                if (dest != null) {
                    dest.rewarded = true;
                    if (rollReward) {
                        rewardService.grantReward(guest, dest);
                    }
                    result.add(dest);
                }
            }
        }

        //无奖励: TargetArea 顺序循环
        List<Integer> targetArea = cfg.getTargetArea();
        if (unrewardedCount > 0 && targetArea != null && !targetArea.isEmpty()) {
            int cursor = 0;
            int safety = unrewardedCount * targetArea.size();
            int added = 0;
            while (added < unrewardedCount && safety-- > 0) {
                int buildingId = targetArea.get(cursor % targetArea.size());
                cursor++;
                DestinationInfo dest = pickBuildingDevice(buildingId, casino);
                if (dest != null) {
                    result.add(dest);
                    added++;
                }
            }
        }

        return result;
    }

    /**
     * 在指定建筑随机挑一个交互设备 (建筑必须已解锁)
     */
    private DestinationInfo pickBuildingDevice(int buildingId, SimCasinoData casino) {
        if (!isBuildingUnlocked(buildingId, casino)) {
            return null;
        }
        List<Integer> devices = configCache.getBuildingDevices(buildingId);
        if (devices == null || devices.isEmpty()) {
            return null;
        }
        int deviceId = devices.get(RandomUtils.randomInt(devices.size()));

        DestinationInfo destinationInfo = new DestinationInfo();
        destinationInfo.buildingId = buildingId;
        destinationInfo.deviceId = deviceId;
        return destinationInfo;
    }

    /**
     * 建筑是否已解锁 (casino.buildingData 包含该 buildingId)
     */
    private boolean isBuildingUnlocked(int buildingId, SimCasinoData casino) {
        Map<Integer, BuildingData> buildingData = casino.getBuildingData();
        if (buildingData == null || buildingData.isEmpty()) {
            return false;
        }
        return buildingData.containsKey(buildingId);
    }

    // ---------------------------------------------------------------------
    // 解锁
    // ---------------------------------------------------------------------

    /**
     * 解锁游客
     */
    public int unlockGuest(SimPlayerContext ctx, int guestId) {
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("解锁游客时，当前赌场数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
            return Code.NOT_FOUND;
        }
        GuestData guest = casino.findGuestData(guestId);
        if (guest == null) {
            VisitorQuestCfg cfg = GameDataManager.getVisitorQuestCfg(guestId);
            if (cfg == null) {
                log.warn("解锁游客时，未找到对应的配置 guestId={}", guestId);
                return Code.NOT_FOUND;
            }
            guest = new GuestData();
            guest.setId(guestId);
            guest.setLevel(1);
            guest.setStar(1);
            casino.addGuest(guest);
        }

        log.info("解锁游客成功 guestId={}", guestId);
        return Code.SUCCESS;
    }

    /**
     * 获取所有游客
     *
     * @param ctx
     */
    public void onAllGuest(SimPlayerContext ctx) {
        ResAllGuest res = new ResAllGuest(Code.SUCCESS);
        try {
            SimCasinoData casinoData = ctx.getCurrentCasino();
            if (casinoData == null) {
                log.warn("获取所有游客失败,当前场景为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            if (casinoData.getGuestMap() != null && !casinoData.getGuestMap().isEmpty()) {
                res.guests = new ArrayList<>();
                for (Map.Entry<Integer, GuestData> en : casinoData.getGuestMap().entrySet()) {
                    GuestData guestData = en.getValue();

                    GuestDetailInfo guestDetailInfo = new GuestDetailInfo();
                    guestDetailInfo.id = guestData.getId();
                    guestDetailInfo.level = guestData.getLevel();
                    guestDetailInfo.star = guestData.getStar();
                    res.guests.add(guestDetailInfo);
                }
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }
}
