package com.jjg.game.sim.service;

import cn.hutool.core.lang.Snowflake;
import com.jjg.game.alliance.constant.AllianceConst;
import com.jjg.game.alliance.service.AllianceEventService;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.ItemListener;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.*;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import com.jjg.game.sim.pb.SimPbConverter;
import com.jjg.game.sim.pb.res.*;
import com.jjg.game.sim.pb.struct.DestinationInfo;
import com.jjg.game.sim.pb.struct.GuestDetailInfo;
import com.jjg.game.sim.pb.struct.GuestInfo;
import com.jjg.game.sim.pb.struct.RecruitItemInfo;
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
public class SimGuestService implements SimPlayerTickListener, ItemListener {
    private static final Logger log = LoggerFactory.getLogger(SimGuestService.class);

    //购买游客 uid 生成器
    private static final Snowflake UID_GENERATOR = new Snowflake(1, 1);

    @Autowired
    private SimConfigCacheService configCache;
    @Autowired
    private SimRewardService rewardService;
    @Autowired
    private SimPackService simPackService;
    @Autowired
    private SimPlayerContextRegistry simPlayerContextRegistry;
    @Autowired
    private AllianceEventService allianceEventService;

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
        //当前场景
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            return;
        }
        //场景配置
        CasinoStatsSheetCfg casinoCfg = configCache.getCasinoStatsSheetCfg(casino.getCasinoId(), casino.getCasinoLevel());
        if (casinoCfg == null) {
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
        //当前场景
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("生成指定游客id失败，当前场景数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
            return;
        }

        VisitorQuestCfg visitorQuestCfg = GameDataManager.getVisitorQuestCfg(guestId);
        if (visitorQuestCfg == null) {
            log.warn("生成指定游客id失败，获取 visitorQuestCfg 配置未找到 playerId={},guestId={}", ctx.playerId(), guestId);
            return;
        }

        //场景配置
        CasinoStatsSheetCfg casinoCfg = configCache.getCasinoStatsSheetCfg(casino.getCasinoId(), casino.getCasinoLevel());
        if (casinoCfg == null) {
            log.warn("生成指定游客id失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={},level={}", ctx.playerId(), casino.getCasinoId(), casino.getCasinoLevel());
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
        //当前场景
        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("生成指定游客quality失败，当前场景数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
            return;
        }
        //场景配置
        CasinoStatsSheetCfg casinoCfg = configCache.getCasinoStatsSheetCfg(casino.getCasinoId(), casino.getCasinoLevel());
        if (casinoCfg == null) {
            log.warn("生成指定游客quality失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={},level={}", ctx.playerId(), casino.getCasinoId(), casino.getCasinoLevel());
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
    // 购买游客 (点击购买后立即生成, 不走定时逻辑; 预生成目的地和奖励, 奖励延后领取时才添加)
    // ---------------------------------------------------------------------

    /**
     * 生成购买游客
     * 预生成目的地序列和奖励并分配唯一 uid; 奖励不添加到玩家身上, 待客户端发起领奖请求时才添加
     *
     * @param ctx
     * @param guestId 购买的游客id
     */
    public void generatePurchasedGuest(SimPlayerContext ctx, int guestId, int count) {
        ResGenPurchasedGuest res = new ResGenPurchasedGuest(Code.SUCCESS);
        if (count < 1) {
            count = 1;
        }

        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("生成购买游客失败，当前场景数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
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
        CasinoStatsSheetCfg casinoCfg = configCache.getCasinoStatsSheetCfg(casino.getCasinoId(), casino.getCasinoLevel());
        if (casinoCfg == null) {
            log.warn("生成购买游客失败，获取 CasinoStatsSheetCfg 配置未找到 playerId={},casinoId={},level={}", ctx.playerId(), casino.getCasinoId(), casino.getCasinoLevel());
            res.code = Code.FAIL;
            ctx.send(res);
            return;
        }

        res.guests = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            GuestData guest = casino.findGuestData(guestId);
            if (guest == null) {
                guest = new GuestData();
                guest.setId(guestId);
                guest.setStar(1);
                guest.setLevel(1);
                casino.addGuest(guest);
            }

            //本次交互次数 (有奖励 + 无奖励)
            VisitorStarCfg starCfg = rewardService.getStarCfg(guestId, guest.getStar());
            int rewardedCount = computeInteractionCount(visitorQuestCfg.getServiceCapacity(), casinoCfg.getProsperity(), starCfg);
            int unrewardedCount = computeInteractionCount(visitorQuestCfg.getBaseServiceCapacity(), casinoCfg.getProsperity(), starCfg);
            if (rewardedCount + unrewardedCount <= 0) {
                log.info("生成购买游客但交互次数为 0, 跳过 playerId={},guestId={}", ctx.playerId(), guestId);
                continue;
            }

            //预生成目的地序列和奖励 (奖励此时不添加到玩家身上, 待领奖请求时才添加)
            List<DestinationInfo> destinations = planDestinations(guest, visitorQuestCfg, casino, rewardedCount, unrewardedCount);
            if (destinations.isEmpty()) {
//            log.warn("生成购买游客失败，目的地序列为空 playerId={},guestId={}", ctx.playerId(), guestId);
                continue;
            }

            PurchasedGuestData data = new PurchasedGuestData();
            data.setUid(UID_GENERATOR.nextIdStr());
            data.setGuestId(guestId);
            data.setStar(guest.getStar());
            data.setLevel(guest.getLevel());

            Map<Integer, DestinationInfo> map = new HashMap<>();
            for (DestinationInfo info : destinations) {
                map.put(info.index, info);
            }
            data.setDestinations(map);
            casino.addPurchasedGuest(data);
            //经营信息: 招商生成一名高级游客即累计一次, 与该游客的目的地数量无关
            ctx.getSimBaseData().addReceptionCount(1);

            res.guests.add(SimPbConverter.toGuestInfo(data, visitorQuestCfg));
            //累加经验
            guest.addExp(configCache.getVisitorLevelCfgMap());
        }
        ctx.send(res);
    }

    /**
     * 领取购买游客单个目的地的奖励 (客户端凭 uid + 目的地序号逐个发起):
     * 奖励已在购买时预生成, 此时才添加到玩家身上 (无奖励交互点返回空); 同一目的地重复领取幂等;
     * 全部目的地领取完毕后移除该购买游客。
     *
     * @param ctx
     * @param uid   购买游客唯一id
     * @param index 目的地序号 (destinations 下标)
     */
    public void claimPurchasedGuestReward(SimPlayerContext ctx, String uid, int index) {
        ResPurchasedGuestReward res = new ResPurchasedGuestReward(Code.SUCCESS);
        res.uid = uid;
        res.index = index;

        if (uid == null || uid.isEmpty()) {
            log.warn("领取购买游客奖励失败，uid参数错误 playerId={},uid={}", ctx.playerId(), uid);
            res.code = Code.NOT_FOUND;
            ctx.send(res);
            return;
        }

        SimCasinoData casino = ctx.getCurrentCasino();
        if (casino == null) {
            log.warn("领取购买游客奖励失败，当前场景数据不存在 playerId={},uid={}", ctx.playerId(), uid);
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
        Map<Integer, DestinationInfo> destinations = data.getDestinations();
        if (destinations == null || index < 0 || index >= destinations.size()) {
            log.warn("领取购买游客奖励失败，目的地序号越界 playerId={},uid={},index={}", ctx.playerId(), uid, index);
            res.code = Code.NOT_FOUND;
            ctx.send(res);
            return;
        }

        DestinationInfo dest = destinations.get(index);
        //首次领取才添加道具; 重复领取直接回已有奖励 (幂等)
        if (!dest.claimed) {
            //奖励已在购买时预生成, 此时才添加到玩家身上
            if (dest.rewards != null && !dest.rewards.isEmpty()) {
                Map<Integer, Long> rewardsMap = new HashMap<>();
                for (ItemInfo itemInfo : dest.rewards) {
                    rewardsMap.merge(itemInfo.itemId, itemInfo.count, Long::sum);
                }
                //添加道具
                simPackService.addItems(ctx, rewardsMap, AddType.SIM_GUEST_REWARDS, null, false);
                //经营信息: 购买游客交互产出金币计入经营收益
                ctx.getSimBaseData().addBusinessIncome(rewardsMap.getOrDefault(ItemUtils.getGoldItemId(), 0L));
            }
            dest.claimed = true;
        }
        res.rewards = dest.rewards;

        //全部目的地领取完毕则移除
        boolean match = destinations.entrySet().stream().allMatch(en -> en.getValue().claimed);
        if (match) {
            casino.removePurchasedGuest(uid);
        }

        log.info("领取购买游客奖励成功 playerId={},guestId={},uid={},index={}", ctx.playerId(), data.getGuestId(), uid, index);
        ctx.send(res);
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

        //规划本次行程的目的地序列 (生成奖励)
        List<DestinationInfo> destinations = planDestinations(guest, visitorQuestCfg, ctx.getCurrentCasino(), rewardedCount, unrewardedCount);
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

        //经营信息: 普通游客不计入高级游客人次; 游客交互产出金币计入玩家经营总收益
        if (!rewardsMap.isEmpty()) {
            ctx.getSimBaseData().addBusinessIncome(rewardsMap.getOrDefault(ItemUtils.getGoldItemId(), 0L));
        }

        //累加经验
        guest.addExp(configCache.getVisitorLevelCfgMap());

        ctx.getCurrentCasino().setLastGenerateTime(now);
        ctx.getCurrentCasino().recordGenerate(now, SimConstant.Common.CAPACITY_WINDOW_MS);
        return SimPbConverter.toGuestInfo(guest, destinations, visitorQuestCfg);
    }

    /**
     * 计算实际来访间隔 (ms)
     * 当场景满员 (10 分钟内生成人数 >= 场景容纳上限) 时降低刷新频率:
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
     * - 有奖励交互点结算奖励到 dest.rewards (是否添加到玩家身上由调用方决定)
     */
    private List<DestinationInfo> planDestinations(GuestData guest, VisitorQuestCfg cfg, SimCasinoData casino, int rewardedCount, int unrewardedCount) {
        int allCount = rewardedCount + unrewardedCount;
        if (allCount < 1) {
            return Collections.emptyList();
        }
        List<DestinationInfo> result = new ArrayList<>(allCount);

        int id = 0;
        //有奖励: InteractionWeight (Map<buildingId, weight>)
        Map<Integer, Integer> interactionWeight = cfg.getInteractionWeight();
        if (rewardedCount > 0 && interactionWeight != null && !interactionWeight.isEmpty()) {
            WeightRandom<Integer> random = WeightRandom.create();
            for (Map.Entry<Integer, Integer> en : interactionWeight.entrySet()) {
                if (en.getValue() == null || en.getValue() <= 0) {
                    continue;
                }
                if (casino.findBuilding(en.getKey()) == null) {
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
                    rewardService.grantReward(guest, dest);
                    dest.index = id;
                    id++;
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
                    dest.index = id;
                    id++;
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
        BuildingData building = casino.findBuilding(buildingId);
        if (building == null) {
            return null;
        }
        BuildingUpgradeTableCfg cfg = configCache.getBuildingUpgradeCfg(buildingId, building.getLevel());
        if (cfg == null || cfg.getUnlockEquipment() == null || cfg.getUnlockEquipment().isEmpty()) {
            return null;
        }
        int deviceId = cfg.getUnlockEquipment().get(RandomUtils.randomInt(cfg.getUnlockEquipment().size()));

        DestinationInfo destinationInfo = new DestinationInfo();
        destinationInfo.buildingId = buildingId;
        destinationInfo.deviceId = deviceId;
        return destinationInfo;
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
            log.warn("解锁游客时，当前场景数据不存在 playerId={},currentCasinoId={}", ctx.playerId(), ctx.getSimBaseData().getCurrentCasinoId());
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
            unlockBonds(ctx, Collections.singletonList(guestId));
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
                    guestDetailInfo.exp = guestData.getExp();
                    res.guests.add(guestDetailInfo);
                }
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 升星游客
     *
     * @param ctx
     * @param guestId
     */
    public void onStarUpGuest(SimPlayerContext ctx, int guestId) {
        ResStarUpGuest res = new ResStarUpGuest(Code.SUCCESS);
        try {
            SimCasinoData casinoData = ctx.getCurrentCasino();
            if (casinoData == null) {
                log.warn("升星游客失败,当前场景为空 playerId={}", ctx.playerId());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            GuestData guestData = casinoData.findGuestData(guestId);
            if (guestData == null) {
                log.warn("升星游客失败,获取该游客数据失败 playerId={},guestId={}", ctx.playerId(), guestId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            VisitorStarCfg cfg = configCache.getVisitorStarCfgByGuest(guestId, guestData.getStar());
            if (cfg == null) {
                log.warn("升星游客失败,获取游客星级配置失败 playerId={},guestId={},star={}", ctx.playerId(), guestId, guestData.getStar());
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            int nextStar = guestData.getStar() + 1;
            VisitorStarCfg nextStarCfg = configCache.getVisitorStarCfgByGuest(guestId, nextStar);
            if (nextStarCfg == null) {
                log.warn("升星游客失败,星级已满 playerId={},guestId={},star={},nextStar={}", ctx.playerId(), guestId, guestData.getStar(), nextStar);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            VisitorQuestCfg visitorQuestCfg = GameDataManager.getVisitorQuestCfg(guestId);
            if (visitorQuestCfg == null) {
                log.warn("升星游客失败,获取游客配置失败 playerId={},guestId={}", ctx.playerId(), guestId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            boolean remove = simPackService.removeItem(ctx, visitorQuestCfg.getDuplicatetoShard().get(1), cfg.getAscend(), AddType.SIM_GUEST_STAR_UP);
            if (!remove) {
                log.warn("升星游客失败,扣除道具失败 playerId={},guestId={}", ctx.playerId(), guestId);
                res.code = Code.NOT_FOUND;
                ctx.send(res);
                return;
            }

            //升星
            guestData.setStar(nextStar);

            res.guestId = guestData.getId();
            res.star = guestData.getStar();
            log.info("升星游客成功 playerId={},guestId={},star={}", ctx.playerId(), guestId, guestData.getStar());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 招募游客
     *
     * @param ctx
     * @param count
     */
    public void onRecruitGuest(SimPlayerContext ctx, int count) {
        ResRecruitGuest res = new ResRecruitGuest(Code.SUCCESS);
        try {
            if (count != 1 && count != 10) {
                log.warn("招募游客失败,次数参数错误 playerId={},count={}", ctx.playerId(), count);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            long now = System.currentTimeMillis();
            PoolListCfg tmpCfg = null;
            for (PoolListCfg cfg : GameDataManager.getPoolListCfgList()) {
                if (cfg.getType() != SimConstant.PoolList.TYPE_GUEST) {
                    continue;
                }

                if (!cfg.getOpen()) {
                    continue;
                }

                if (cfg.getTime_start() != null && !cfg.getTime_start().isEmpty() && cfg.getTime_end() != null && !cfg.getTime_end().isEmpty()) {
                    long startTime = TimeHelper.getTimeMillisBy(cfg.getTime_start());
                    long endTime = TimeHelper.getTimeMillisBy(cfg.getTime_end());
                    if (startTime >= endTime) {
                        continue;
                    }
                    if (now >= startTime && now <= endTime) {
                        tmpCfg = cfg;
                        break;
                    }
                } else {
                    tmpCfg = cfg;
                    break;
                }
            }

            if (tmpCfg == null) {
                log.warn("招募游客失败,获取配置失败 playerId={},count={}", ctx.playerId(), count);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            WeightRandom<List<Integer>> poolRand = configCache.getPoolRand(tmpCfg.getDropItem());
            if (poolRand == null) {
                log.warn("招募游客失败,获取配置失败1 playerId={},count={}", ctx.playerId(), count);
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            if (tmpCfg.getDrawCost() != null && !tmpCfg.getDrawCost().isEmpty()) {
                Map<Integer, Long> costMap = tmpCfg.getDrawCost();
                if (count > 1) {
                    costMap = new HashMap<>();
                    for (Map.Entry<Integer, Long> en : tmpCfg.getDrawCost().entrySet()) {
                        costMap.put(en.getKey(), en.getValue() * count);
                    }
                }
                boolean remove = simPackService.removeItems(ctx, costMap, AddType.SIM_GUEST_RECRUIT, null);
                if (!remove) {
                    log.warn("招募游客失败,扣除道具失败 playerId={},count={},poolId={}", ctx.playerId(), count, tmpCfg.getId());
                    res.code = Code.PARAM_ERROR;
                    ctx.send(res);
                    return;
                }
            }

            Map<Integer, Long> addAllItems = new HashMap<>();
            List<RecruitItemInfo> recruitItems = new ArrayList<>();

            Map<Integer, Integer> addGuest = new HashMap<>();


            for (int i = 0; i < count; i++) {
                List<Integer> next = poolRand.next();
                if (next == null || next.size() < 3) {
                    log.warn("招募游客失败,获取配置失败2 playerId={},count={},i={}", ctx.playerId(), count, i);
                    return;
                }

                VisitorQuestCfg visitorQuestCfg = configCache.getVisitorQuestCfgByItemId(next.get(1));
                if (visitorQuestCfg == null) {
                    log.warn("招募游客失败,根据itemId获取VisitorQuestCfg失败 playerId={},count={},itemId={},i={}", ctx.playerId(), count, next.get(1), i);
                    return;
                }

                int rewardCount = next.get(2);
                for (int j = 0; j < rewardCount; j++) {
                    RecruitItemInfo re = new RecruitItemInfo();
                    re.itemId = visitorQuestCfg.getDuplicatetoShard().get(0);
                    re.count = visitorQuestCfg.getDuplicatetoShard().get(2);

                    GuestData guestData = ctx.getCurrentCasino().findGuestData(visitorQuestCfg.getId());
                    if (guestData == null) {
                        guestData = new GuestData();
                        guestData.setId(visitorQuestCfg.getId());
                        guestData.setStar(1);
                        guestData.setLevel(1);
                        ctx.getCurrentCasino().addGuest(guestData);

                        addGuest.merge(visitorQuestCfg.getId(), 1, Integer::sum);
                    } else { //分解成碎片
                        List<Integer> tmpList = visitorQuestCfg.getDuplicatetoShard();
                        if (tmpList != null && !tmpList.isEmpty()) {
                            int itemId = tmpList.get(1);
                            long itemCount = tmpList.get(2).longValue();

                            addAllItems.merge(itemId, itemCount, Long::sum);
                            re.breakDown = true;
                        }
                    }
                    recruitItems.add(re);
                }
            }

            if (!addAllItems.isEmpty()) {
                CommonResult<SimItemOperationResult> simItemOperationResultCommonResult = simPackService.addItems(ctx, addAllItems, AddType.SIM_GUEST_RECRUIT, count + "", false);
                if (!simItemOperationResultCommonResult.success()) {
                    log.warn("招募游客后添加碎片道具失败 playerId={},count={},code={}", ctx.playerId(), count, simItemOperationResultCommonResult.code);
                    res.code = simItemOperationResultCommonResult.code;
                    ctx.send(res);
                    return;
                }
            }

            if (!addGuest.isEmpty()) {
                List<Integer> guestIds = new ArrayList<>();
                for (Map.Entry<Integer, Integer> en : addGuest.entrySet()) {
                    guestIds.add(en.getKey());
                }
                unlockBonds(ctx, guestIds);
            }

            res.shardInfos = recruitItems;
            //联盟任务: 卡池抽奖次数 (param=卡池ID, 供 0=任意/指定卡池 过滤; 10 连计为 10 次)
            allianceEventService.onEvent(ctx.playerId(), AllianceConst.TaskConditionType.POOL_DRAW_TIMES, tmpCfg.getId(), count);
            log.info("招募游客成功 playerId={},count={},newEmployee={},addAllItems={}", ctx.playerId(), count, addGuest, addAllItems);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 获取游客羁绊
     *
     * @param ctx
     */
    public void onBonds(SimPlayerContext ctx) {
        ResGuestBonds res = new ResGuestBonds(Code.SUCCESS);
        try {
            if (ctx.getCurrentCasino().getGuestBondsSet() != null && !ctx.getCurrentCasino().getGuestBondsSet().isEmpty()) {
                res.bonds = new ArrayList<>(ctx.getCurrentCasino().getGuestBondsSet());

                Map<Integer, Long> map = new HashMap<>();
                for (int id : ctx.getCurrentCasino().getGuestBondsSet()) {
                    VisitorBondsCfg cfg = GameDataManager.getVisitorBondsCfg(id);
                    if (cfg == null || cfg.getReward() == null || cfg.getReward().isEmpty()) {
                        continue;
                    }
                    for (Map.Entry<Integer, Long> en : cfg.getReward().entrySet()) {
                        map.merge(en.getKey(), en.getValue(), Long::sum);
                    }
                }
                res.rewards = ItemUtils.buildItemInfo(map);
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    /**
     * 解锁羁绊
     *
     * @param ctx
     */
    private void unlockBonds(SimPlayerContext ctx, List<Integer> guestIds) {
        if (guestIds == null || guestIds.isEmpty()) {
            return;
        }

        Map<Integer, Long> addItems = new HashMap<>();

        for (int guestId : guestIds) {
            Set<Integer> bondsIds = configCache.getBondsByGuestId(guestId);
            if (bondsIds == null || bondsIds.isEmpty()) {
                continue;
            }

            bonds:
            for (int bondsCfgId : bondsIds) {
                //是否已解锁这个羁绊
                if (ctx.getCurrentCasino().containsGuestBonds(bondsCfgId)) {
                    continue;
                }

                //获取羁绊配置
                VisitorBondsCfg visitorBondsCfg = GameDataManager.getVisitorBondsCfg(bondsCfgId);
                if (visitorBondsCfg == null) {
                    continue;
                }

                if (visitorBondsCfg.getMembers() != null && !visitorBondsCfg.getMembers().isEmpty()) {
                    for (int memberGuestId : visitorBondsCfg.getMembers()) {
                        GuestData guestData = ctx.getCurrentCasino().findGuestData(memberGuestId);
                        if (guestData == null) {
                            continue bonds;
                        }
                    }
                }
                //添加羁绊
                ctx.getCurrentCasino().addGuestBonds(visitorBondsCfg.getId());

                if (visitorBondsCfg.getReward() != null && !visitorBondsCfg.getReward().isEmpty()) {
                    for (Map.Entry<Integer, Long> en : visitorBondsCfg.getReward().entrySet()) {
                        addItems.merge(en.getKey(), en.getValue(), Long::sum);
                    }
                }
                log.info("成功解锁羁绊 playerId={},bondsId={}", ctx.playerId(), visitorBondsCfg.getId());
            }
        }

        if (!addItems.isEmpty()) {
            CommonResult<SimItemOperationResult> addResult = simPackService.addItems(ctx, addItems, AddType.SIM_UNLOCK_BONDS, null, true);
            if (addResult.success()) {
                log.info("解锁羁绊添加道具成功 playerId={},addItems={}", ctx.playerId(), addItems);
            } else {
                log.warn("解锁羁绊添加道具失败 playerId={},addItems={},code={}", ctx.playerId(), addItems, addResult.code);
            }
        }
    }

    /**
     * 获取卡池
     *
     * @param ctx
     */
    public void onPool(SimPlayerContext ctx) {
        ResGuestPool res = new ResGuestPool(Code.SUCCESS);
        try {
            long now = System.currentTimeMillis();
            PoolListCfg tmpCfg = null;
            for (PoolListCfg cfg : GameDataManager.getPoolListCfgList()) {
                if (cfg.getType() != SimConstant.PoolList.TYPE_GUEST) {
                    continue;
                }

                if (!cfg.getOpen()) {
                    continue;
                }

                if (cfg.getTime_start() != null && !cfg.getTime_start().isEmpty() && cfg.getTime_end() != null && !cfg.getTime_end().isEmpty()) {
                    long startTime = TimeHelper.getTimeMillisBy(cfg.getTime_start());
                    long endTime = TimeHelper.getTimeMillisBy(cfg.getTime_end());
                    if (startTime >= endTime) {
                        continue;
                    }
                    if (now >= startTime && now <= endTime) {
                        tmpCfg = cfg;
                        break;
                    }
                } else {
                    tmpCfg = cfg;
                    break;
                }
            }

            if (tmpCfg == null) {
                log.warn("获取游客卡池失败1,playerId={}", ctx.playerId());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            VisitorPoolCfg visitorPoolCfg = GameDataManager.getVisitorPoolCfg(tmpCfg.getDropItem());
            if (visitorPoolCfg == null || visitorPoolCfg.getDetailedDropItem() == null) {
                log.warn("获取游客卡池失败2,playerId={},dropItem={}", ctx.playerId(), tmpCfg.getDropItem());
                res.code = Code.PARAM_ERROR;
                ctx.send(res);
                return;
            }

            res.guestIds = new ArrayList<>();
            for (List<Integer> list : visitorPoolCfg.getDetailedDropItem()) {
                res.guestIds.add(list.get(1));
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        ctx.send(res);
    }

    @Override
    public void useItem(Player player, int itemId, long useItemCount, int selectItemId, long finalSelectItemCount, Map<Integer, Long> getItemsMap) {
        try {
            if (selectItemId > 0) {
                VisitorQuestCfg cfg = null;
                if (itemId == SimConstant.Item.ID_BATCH_GENERATE_SPECIFY_ID_GUEST) {
                    cfg = configCache.getVisitorQuestCfgByItemId(selectItemId);
                } else if (itemId == SimConstant.Item.ID_BATCH_GENERATE_SPECIFY_QUALITY_GUEST) {
                    Integer qulity = configCache.queryGuestQuality(selectItemId);
                    if (qulity != null) {
                        List<VisitorQuestCfg> tmpList = configCache.getVisitorQuestCfgList(qulity);
                        if (tmpList == null || tmpList.isEmpty()) {
                            return;
                        }
                        cfg = RandomUtils.randomEle(tmpList);
                    }
                } else {
                    return;
                }

                if (cfg == null) {
                    return;
                }

                SimPlayerContext context = this.simPlayerContextRegistry.getContext(player.getId());
                if (context == null) {
                    log.warn("使用道具后生成游客失败，获取context 失败 playerId={}", player.getId());
                    return;
                }

                if (itemId == SimConstant.Item.ID_BATCH_GENERATE_SPECIFY_ID_GUEST) {
                    batchGenerateSpecifyIdGuest(context, cfg.getId(), (int) finalSelectItemCount);
                } else {
                    batchGenerateSpecifyQualityGuest(context, cfg.getQuality(), (int) finalSelectItemCount);
                }
            } else if (getItemsMap != null && !getItemsMap.isEmpty()) {
                SimPlayerContext context = this.simPlayerContextRegistry.getContext(player.getId());
                if (context == null) {
                    log.warn("使用道具后生成游客失败，获取context 失败 playerId={}", player.getId());
                    return;
                }

                for (Map.Entry<Integer, Long> en : getItemsMap.entrySet()) {
                    int getItemId = en.getKey();
                    int count = (int) (en.getValue() * useItemCount);
                    if (count <= 0) {
                        continue;
                    }

                    //品质道具 -> 生成对应品质的随机游客
                    Integer quality = configCache.queryGuestQuality(getItemId);
                    if (quality != null) {
                        batchGenerateSpecifyQualityGuest(context, quality, count);
                        continue;
                    }

                    //游客卡道具 -> 生成指定游客
                    VisitorQuestCfg cfg = configCache.getVisitorQuestCfgByItemId(getItemId);
                    if (cfg != null) {
                        batchGenerateSpecifyIdGuest(context, cfg.getId(), count);
                    }
                }
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
