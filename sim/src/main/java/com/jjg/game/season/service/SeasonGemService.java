package com.jjg.game.season.service;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.SeasonGemCfg;
import com.jjg.game.sampledata.bean.SeasonGemCraftCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.season.data.SeasonBatchCraftResult;
import com.jjg.game.season.data.SeasonCraftResult;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.data.SeasonSlotsSessionData;
import com.jjg.game.sim.listener.SimPlayerTickListener;
import com.jjg.game.sim.service.SimAutoSaveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntPredicate;

/**
 * 宝石镶嵌与配置驱动的合成逻辑。
 * <p>镶嵌为真转移：宝石从背包进入槽位，卸下/替换时再回到背包；背包与槽位互斥。</p>
 */
@Service
public class SeasonGemService implements SimPlayerTickListener {
    private static final Logger log = LoggerFactory.getLogger(SeasonGemService.class);
    private static final int SLOTS_PER_TYPE = 3;
    private static final int GEM_TYPE_COUNT = 3;

    private final SeasonConfigService configService;
    private final SeasonEconomyService economyService;
    private final PlayerPackService playerPackService;
    private final SimAutoSaveService autoSaveService;
    private final IntPredicate successRoll;

    @Autowired
    public SeasonGemService(SeasonConfigService configService,
                            SeasonEconomyService economyService, PlayerPackService playerPackService,
                            SimAutoSaveService autoSaveService) {
        this(configService, economyService, playerPackService, autoSaveService,
                RandomUtils::getRandomBoolean100);
    }

    public SeasonGemService(SeasonConfigService configService,
                            SeasonEconomyService economyService, PlayerPackService playerPackService,
                            SimAutoSaveService autoSaveService,
                            IntPredicate successRoll) {
        this.configService = configService;
        this.economyService = economyService;
        this.playerPackService = playerPackService;
        this.autoSaveService = autoSaveService;
        this.successRoll = successRoll;
    }

    public CommonResult<Map<Integer, Integer>> equip(SimPlayerContext ctx, int slot, int itemId) {
        if (slot <= 0 || slot > SLOTS_PER_TYPE * GEM_TYPE_COUNT) {
            log.warn("赛季宝石槽位错误 playerId={},slot={}", ctx.playerId(), slot);
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        int currentItemId = data.getEquippedGems().getOrDefault(slot, 0);
        settleOnlineEarnings(ctx, System.currentTimeMillis());
        if (itemId == 0) {
            if (currentItemId <= 0) {
                return new CommonResult<>(Code.SUCCESS, Map.copyOf(data.getEquippedGems()));
            }
            CommonResult<?> restored = playerPackService.addItems(ctx.playerId(), Map.of(currentItemId, 1L),
                    AddType.ITEM_EXCHANGE, "season-gem-unequip", true);
            if (!restored.success()) {
                log.warn("赛季宝石卸下回包失败 playerId={},itemId={},code={}",
                        ctx.playerId(), currentItemId, restored.code);
                return new CommonResult<>(restored.code);
            }
            data.getEquippedGems().remove(slot);
            autoSaveService.enqueueSave(data);
            return new CommonResult<>(Code.SUCCESS, Map.copyOf(data.getEquippedGems()));
        }
        if (!isSlotUnlocked(data, slot)) {
            log.warn("赛季宝石槽位未开放 playerId={},seasonId={},slot={}",
                    ctx.playerId(), data.getSeasonId(), slot);
            return new CommonResult<>(Code.NOT_UNLOCKED);
        }
        SeasonGemCfg gem = configService.gemByItemId(itemId);
        int requiredType = (slot - 1) / SLOTS_PER_TYPE + 1;
        if (gem == null || gem.getType() != requiredType) {
            log.warn("赛季宝石形状与槽位不匹配 playerId={},slot={},itemId={}", ctx.playerId(), slot, itemId);
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        if (currentItemId == itemId) {
            return new CommonResult<>(Code.SUCCESS, Map.copyOf(data.getEquippedGems()));
        }
        if (!playerPackService.removeItems(ctx.getPlayer(), Map.of(itemId, 1L), AddType.ITEM_EXCHANGE, "season-gem-equip").success()) {
            log.warn("赛季宝石镶嵌失败,道具不足 playerId={},itemId={}", ctx.playerId(), itemId);
            return new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        }
        if (currentItemId > 0) {
            CommonResult<?> restored = playerPackService.addItems(ctx.playerId(), Map.of(currentItemId, 1L),
                    AddType.ITEM_EXCHANGE, "season-gem-unequip", true);
            if (!restored.success()) {
                playerPackService.addItems(ctx.playerId(), Map.of(itemId, 1L), AddType.FAIL_ROLLBACK, "season-gem-equip", true);
                log.warn("赛季宝石替换回包失败 playerId={},oldItemId={},code={}",
                        ctx.playerId(), currentItemId, restored.code);
                return new CommonResult<>(restored.code);
            }
        }
        data.getEquippedGems().put(slot, itemId);
        autoSaveService.enqueueSave(data);
        return new CommonResult<>(Code.SUCCESS, Map.copyOf(data.getEquippedGems()));
    }

    /**
     * 为赛季 slots 会话生成宝石效果快照。
     *
     * <p>只使用与当前 {@code gameType} 匹配的已镶嵌宝石。同一种宝石可以镶嵌在多个槽位，
     * 因此概率权重按槽位逐个累加；下注额按每行三个槽位内相同宝石数量解锁，最后取去重并集。</p>
     */
    public SeasonSlotsSessionData buildSlotsSessionData(SeasonPlayerData data, int gameType) {
        SeasonSlotsSessionData result = new SeasonSlotsSessionData();
        if (data == null) {
            return result;
        }
        result.setSeasonCoin(data.getSeasonCoin());
        if (data.getEquippedGems().isEmpty()) {
            return result;
        }

        LinkedHashSet<Long> unlockedBets = new LinkedHashSet<>();
        Map<Integer, Map<Integer, Integer>> betGemCountsByRow = new LinkedHashMap<>();
        Map<Integer, Integer> libTypeWeightDelta = new HashMap<>();
        Map<Integer, Map<Integer, Integer>> sectionWeightDelta = new HashMap<>();

        // 固定按槽位聚合，使跨节点快照及下注列表顺序稳定，便于排障和回放。
        data.getEquippedGems().entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(entry -> {
                    SeasonGemCfg cfg = configService.gemByItemId(entry.getValue());
                    if (cfg == null || cfg.getGameID() != gameType) {
                        return;
                    }
                    int row = (entry.getKey() - 1) / SLOTS_PER_TYPE;
                    betGemCountsByRow.computeIfAbsent(row, ignored -> new LinkedHashMap<>())
                            .merge(entry.getValue(), 1, Integer::sum);
                    mergeDelta(libTypeWeightDelta, cfg.getSpecialMode());
                    mergeSectionDelta(sectionWeightDelta, cfg.getWinRate());
                    mergeSectionDelta(sectionWeightDelta, cfg.getSpecialModeProbUp());
                });

        betGemCountsByRow.values().forEach(gemCounts ->
                gemCounts.forEach((itemId, count) -> {
                    SeasonGemCfg cfg = configService.gemByItemId(itemId);
                    if (cfg == null) {
                        return;
                    }
                    Map<Integer, List<Long>> betByCount = cfg.getBet();
                    if (betByCount == null) {
                        return;
                    }
                    List<Long> bets = betByCount.get(count);
                    if (bets != null) {
                        bets.stream().filter(Objects::nonNull).forEach(unlockedBets::add);
                    }
                }));

        result.setBet(new ArrayList<>(unlockedBets));
        result.setLibTypeWeightDelta(libTypeWeightDelta);
        result.setSectionWeightDelta(sectionWeightDelta);
        return result;
    }

    private static void mergeDelta(Map<Integer, Integer> target, Map<Integer, Integer> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((key, delta) -> {
            if (key != null && delta != null) {
                target.merge(key, delta, Integer::sum);
            }
        });
    }

    private static void mergeSectionDelta(Map<Integer, Map<Integer, Integer>> target,
                                          Map<Integer, Map<Integer, Integer>> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((libType, sectionDelta) -> {
            if (libType == null || sectionDelta == null || sectionDelta.isEmpty()) {
                return;
            }
            Map<Integer, Integer> targetSection = target.computeIfAbsent(libType, ignored -> new HashMap<>());
            mergeDelta(targetSection, sectionDelta);
        });
    }

    /**
     * GemCount 在三种形状间均匀开孔：3/6/9 分别表示每种形状开放 1/2/3 个孔。
     */
    boolean isSlotUnlocked(SeasonPlayerData data, int slot) {
        SeasonStartCfg season = data == null ? null : configService.season(data.getSeasonId());
        if (season == null || slot <= 0 || slot > SLOTS_PER_TYPE * GEM_TYPE_COUNT) {
            return false;
        }
        int unlocked = Math.max(0, Math.min(season.getGemCount(), SLOTS_PER_TYPE * GEM_TYPE_COUNT));
        int typeIndex = (slot - 1) / SLOTS_PER_TYPE;
        int positionInType = (slot - 1) % SLOTS_PER_TYPE;
        int perType = unlocked / GEM_TYPE_COUNT;
        int remainder = unlocked % GEM_TYPE_COUNT;
        return positionInType < perType + (typeIndex < remainder ? 1 : 0);
    }

    /**
     * 宝石品质取自其道具的 Item 表 quality 字段; 配置缺失返回 0。
     */
    private int gemQuality(SeasonGemCfg gem) {
        ItemCfg item = GameDataManager.getItemCfg(gem.getItemId());
        return item == null ? 0 : item.getQuality();
    }

    /**
     * 发起并完成一次宝石合成。成功时产出宝石；失败时随机保留一种材料宝石，
     * 通过净扣料一次完成消耗与返还。
     */
    public CommonResult<SeasonCraftResult> craft(SimPlayerContext ctx, List<Integer> itemIds) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        CommonResult<CraftContext> resolved = resolveCraft(ctx, itemIds);
        if (!resolved.success()) {
            return new CommonResult<>(resolved.code);
        }
        CraftContext craftCtx = resolved.data;
        SeasonGemCraftCfg craft = craftCtx.craft;
        if (data.getSeasonCoin() < craft.getMergeCost()) {
            log.warn("赛季宝石合成赛季币不足 playerId={},need={},have={}",
                    ctx.playerId(), craft.getMergeCost(), data.getSeasonCoin());
            return new CommonResult<>(Code.NOT_ENOUGH);
        }

        CommonResult<CraftOutcome> rolled = rollCraft(craftCtx);
        if (!rolled.success()) {
            return new CommonResult<>(rolled.code);
        }
        int code = commitCraft(ctx, data, rolled.data.requiredItems, rolled.data.consumedItems,
                rolled.data.resultItems, craft.getMergeCost(), "season-gem-craft");
        return code == Code.SUCCESS
                ? new CommonResult<>(Code.SUCCESS, rolled.data.result)
                : new CommonResult<>(code);
    }

    /**
     * 按请求开始时的背包快照批量合成指定品质。低品质产出不会在本次操作中继续参与高品质合成。
     */
    public CommonResult<SeasonBatchCraftResult> craftBatch(SimPlayerContext ctx, List<Integer> qualities) {
        if (qualities == null || qualities.isEmpty()) {
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        List<Integer> selected = new ArrayList<>(new LinkedHashSet<>(qualities));
        if (selected.stream().anyMatch(Objects::isNull)) {
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        selected.sort(Integer::compareTo);

        Map<Integer, SeasonGemCraftCfg> crafts = new LinkedHashMap<>();
        for (int quality : selected) {
            SeasonGemCraftCfg craft = configService.craftForQuality(quality);
            if (craft == null || craft.getCostAmount() <= 0 || craft.getMergeCost() < 0) {
                log.warn("批量合成宝石品质配置错误 playerId={},quality={}", ctx.playerId(), quality);
                return new CommonResult<>(Code.PARAM_ERROR);
            }
            crafts.put(quality, craft);
        }

        PlayerPack pack = playerPackService.getFromAllDB(ctx.playerId());
        if (pack == null) {
            return new CommonResult<>(Code.NOT_FOUND);
        }
        Map<Integer, Map<Integer, Long>> materialsByQuality = new LinkedHashMap<>();
        selected.forEach(quality -> materialsByQuality.put(quality, new LinkedHashMap<>()));
        configService.gems().stream()
                .sorted(Comparator.comparingInt(SeasonGemCfg::getItemId))
                .forEach(gem -> {
                    Map<Integer, Long> materials = materialsByQuality.get(gemQuality(gem));
                    long count = pack.getItemCount(gem.getItemId());
                    if (materials != null && count > 0) {
                        materials.put(gem.getItemId(), count);
                    }
                });

        SeasonPlayerData data = ctx.getSeasonPlayerData();
        BatchAccumulator batch = new BatchAccumulator();
        boolean hasMaterials = false;
        for (Map.Entry<Integer, SeasonGemCraftCfg> entry : crafts.entrySet()) {
            SeasonGemCraftCfg craft = entry.getValue();
            Map<Integer, Long> materials = materialsByQuality.get(entry.getKey());
            long materialCount = materials.values().stream().mapToLong(Long::longValue).sum();
            long possibleCrafts = materialCount / craft.getCostAmount();
            if (possibleCrafts <= 0) {
                continue;
            }
            hasMaterials = true;
            long affordableCrafts = craft.getMergeCost() == 0
                    ? possibleCrafts
                    : (data.getSeasonCoin() - batch.totalCost) / craft.getMergeCost();
            int craftLimit = (int) Math.min(Integer.MAX_VALUE, Math.min(possibleCrafts, affordableCrafts));
            for (List<Integer> group : materialGroups(materials, craft.getCostAmount(), craftLimit)) {
                CommonResult<CraftContext> resolved = resolveCraft(ctx, group);
                if (!resolved.success()) {
                    return new CommonResult<>(resolved.code);
                }
                CommonResult<CraftOutcome> rolled = rollCraft(resolved.data);
                if (!rolled.success()) {
                    return new CommonResult<>(rolled.code);
                }
                batch.add(rolled.data, craft.getMergeCost());
            }
        }
        if (batch.craftCount == 0) {
            return new CommonResult<>(hasMaterials ? Code.NOT_ENOUGH : Code.NOT_ENOUGH_ITEM);
        }

        int code = commitCraft(ctx, data, batch.requiredItems, batch.consumedItems,
                batch.resultItems, batch.totalCost, "season-gem-craft-batch");
        if (code != Code.SUCCESS) {
            return new CommonResult<>(code);
        }
        SeasonBatchCraftResult result = new SeasonBatchCraftResult();
        result.setCraftCount(batch.craftCount);
        result.setSuccessCount(batch.successCount);
        result.setCraftCountsByQuality(batch.craftCountsByQuality);
        result.setConsumedItems(batch.consumedItems);
        result.setResultItems(batch.resultItems);
        result.setFailKeepItems(batch.failKeepItems);
        return new CommonResult<>(Code.SUCCESS, result);
    }

    /**
     * 校验合成材料并解析出合成上下文(材料构成、配置、品质等)，不含赛季币校验。
     */
    private CommonResult<CraftContext> resolveCraft(SimPlayerContext ctx, List<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            log.warn("赛季宝石合成参数为空 playerId={}", ctx.playerId());
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        SeasonGemCfg first = configService.gemByItemId(itemIds.getFirst());
        if (first == null) {
            log.warn("赛季宝石合成配置不存在 playerId={},itemId={}", ctx.playerId(), itemIds.getFirst());
            return new CommonResult<>(Code.NOT_FOUND);
        }
        int quality = gemQuality(first);
        SeasonGemCraftCfg craft = configService.craftForQuality(quality);
        if (craft == null || itemIds.size() != craft.getCostAmount()) {
            log.warn("赛季宝石合成数量或配置错误 playerId={},quality={},count={}",
                    ctx.playerId(), quality, itemIds.size());
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        Map<Integer, Long> input = new HashMap<>();
        boolean allSame = true;
        for (int itemId : itemIds) {
            SeasonGemCfg gem = configService.gemByItemId(itemId);
            if (gem == null || gemQuality(gem) != quality) {
                log.warn("赛季宝石合成品质不一致 playerId={},quality={},count={}",
                        ctx.playerId(), quality, itemIds.size());
                return new CommonResult<>(Code.PARAM_ERROR);
            }
            allSame &= itemId == first.getItemId();
            input.merge(itemId, 1L, Long::sum);
        }
        return new CommonResult<>(Code.SUCCESS, new CraftContext(quality, craft, first, allSame, input));
    }

    private CommonResult<CraftOutcome> rollCraft(CraftContext craftCtx) {
        SeasonGemCraftCfg craft = craftCtx.craft;
        boolean success = successRoll.test(craft.getMergeSuccessRate());
        SeasonCraftResult result = new SeasonCraftResult();
        result.setMaterialQuality(craftCtx.quality);
        result.setSuccess(success);
        Map<Integer, Long> consumed = new HashMap<>(craftCtx.input);
        Map<Integer, Long> rewards = new HashMap<>();
        Map<Integer, Long> kept = new HashMap<>();
        if (!success) {
            int keepItemId = RandomUtils.randomEle(expandItems(craftCtx.input));
            long keepCount = Math.min(Math.max(0, craft.getFailKeepAmount()), consumed.get(keepItemId));
            consumed.computeIfPresent(keepItemId,
                    (itemId, count) -> count > keepCount ? count - keepCount : null);
            if (keepCount > 0) {
                kept.put(keepItemId, keepCount);
            }
            result.setFailKeepItemId(keepItemId);
            return new CommonResult<>(Code.SUCCESS,
                    new CraftOutcome(result, craftCtx.input, consumed, rewards, kept));
        }

        SeasonGemCfg output = chooseOutput(craftCtx.first, craftCtx.allSame, craft.getSuccessGem());
        if (output == null) {
            log.warn("赛季宝石合成产出配置错误 quality={}", craftCtx.quality);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        int count = outputCount(output.getId(), craft.getSuccessGem());
        rewards.put(output.getItemId(), (long) count);
        result.setResultItemId(output.getItemId());
        result.setResultCount(count);
        return new CommonResult<>(Code.SUCCESS,
                new CraftOutcome(result, craftCtx.input, consumed, rewards, kept));
    }

    private int commitCraft(SimPlayerContext ctx, SeasonPlayerData data, Map<Integer, Long> required,
                            Map<Integer, Long> consumed, Map<Integer, Long> rewards,
                            long coinCost, String desc) {
        if (data.getSeasonCoin() < coinCost) {
            return Code.NOT_ENOUGH;
        }
        CommonResult<?> exchange = playerPackService.exchangePackItems(ctx.getPlayer(), required,
                consumed, rewards, AddType.ITEM_EXCHANGE, desc);
        if (!exchange.success()) {
            log.warn("赛季宝石合成兑换道具失败 playerId={},code={}", ctx.playerId(), exchange.code);
            return exchange.code;
        }
        data.setSeasonCoin(data.getSeasonCoin() - coinCost);
        autoSaveService.enqueueSave(data);
        return Code.SUCCESS;
    }

    private List<List<Integer>> materialGroups(Map<Integer, Long> materials, int costAmount, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        Map<Integer, Long> remaining = new LinkedHashMap<>(materials);
        List<List<Integer>> groups = new ArrayList<>(limit);
        for (Map.Entry<Integer, Long> entry : remaining.entrySet()) {
            long sameGroups = Math.min(entry.getValue() / costAmount, limit - groups.size());
            for (long i = 0; i < sameGroups; i++) {
                groups.add(new ArrayList<>(Collections.nCopies(costAmount, entry.getKey())));
            }
            entry.setValue(entry.getValue() - sameGroups * costAmount);
            if (groups.size() == limit) {
                return groups;
            }
        }

        List<Integer> mixed = expandItems(remaining);
        for (int offset = 0; offset + costAmount <= mixed.size() && groups.size() < limit;
             offset += costAmount) {
            groups.add(new ArrayList<>(mixed.subList(offset, offset + costAmount)));
        }
        return groups;
    }

    private List<Integer> expandItems(Map<Integer, Long> items) {
        List<Integer> result = new ArrayList<>();
        items.forEach((itemId, count) -> {
            for (long i = 0; i < count; i++) {
                result.add(itemId);
            }
        });
        return result;
    }

    @Override
    public void onTick(SimPlayerContext ctx, long now) {
        settleOnlineEarnings(ctx, now);
    }

    /**
     * 结算所有已镶嵌宝石新增的整分钟在线收益。同一次 tick 先聚合再统一入账，
     * 不逐宝石调用经济入口，也不在分钟 tick 中主动写库。
     */
    void settleOnlineEarnings(SimPlayerContext ctx, long now) {
        if (ctx == null || ctx.getSeasonPlayerData() == null) {
            return;
        }
        if (ctx.getPlayerController() == null) {
            ctx.setLastGemEarningTime(0);
            return;
        }

        SeasonPlayerData data = ctx.getSeasonPlayerData();
        long last = ctx.getLastGemEarningTime();
        if (last <= 0) {
            prepareEarningDay(data, dailyKey(Math.addExact(now, data.getGmTimeOffset())));
            ctx.setLastGemEarningTime(now);
            return;
        }
        if (now <= last) {
            return;
        }

        long reward = 0;
        long segmentStart = last;
        long offset = data.getGmTimeOffset();
        ZoneId zone = ZoneId.systemDefault();
        while (segmentStart < now) {
            long seasonTime = Math.addExact(segmentStart, offset);
            LocalDate date = Instant.ofEpochMilli(seasonTime).atZone(zone).toLocalDate();
            prepareEarningDay(data, dailyKey(date));

            long nextDaySystemTime = Math.subtractExact(
                    date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), offset);
            long segmentEnd = Math.min(now, nextDaySystemTime);
            if (segmentEnd <= segmentStart) {
                break;
            }
            reward = Math.addExact(reward, accrue(data, segmentEnd - segmentStart));
            segmentStart = segmentEnd;
        }
        ctx.setLastGemEarningTime(now);
        if (reward > 0) {
            economyService.addBalance(data, reward);
        }
    }

    /** 登出前结清在线时段并清空游标，防止快速重登复活内存上下文时把离线间隔算入。 */
    void stopOnlineEarnings(SimPlayerContext ctx, long now) {
        settleOnlineEarnings(ctx, now);
        if (ctx != null) {
            ctx.setLastGemEarningTime(0);
        }
    }

    private long accrue(SeasonPlayerData data, long elapsed) {
        if (elapsed <= 0 || data.getEquippedGems().isEmpty()) {
            return 0;
        }
        long reward = 0;
        Map<String, Long> earningMillis = data.getDailyGemEarningMillis();
        for (Map.Entry<Integer, Integer> entry : data.getEquippedGems().entrySet()) {
            SeasonGemCfg cfg = configService.gemByItemId(entry.getValue());
            if (cfg == null || cfg.getMaxEarningTime() <= 0 || cfg.getStatBoost() <= 0) {
                continue;
            }
            long maxMillis = Math.multiplyExact((long) cfg.getMaxEarningTime(), TimeHelper.ONE_MINUTE_OF_MILLIS);
            String earningKey = entry.getKey() + ":" + entry.getValue();
            long oldMillis = Math.max(0, Math.min(earningMillis.getOrDefault(earningKey, 0L), maxMillis));
            long newMillis = oldMillis + Math.min(elapsed, maxMillis - oldMillis);
            if (newMillis == oldMillis) {
                continue;
            }
            earningMillis.put(earningKey, newMillis);
            long newMinutes = newMillis / TimeHelper.ONE_MINUTE_OF_MILLIS;
            long oldMinutes = oldMillis / TimeHelper.ONE_MINUTE_OF_MILLIS;
            reward = Math.addExact(reward,
                    Math.multiplyExact(newMinutes - oldMinutes, (long) cfg.getStatBoost()));
        }
        return reward;
    }

    private void prepareEarningDay(SeasonPlayerData data, int dailyKey) {
        if (data.getGemEarningDailyKey() == dailyKey) {
            return;
        }
        data.setGemEarningDailyKey(dailyKey);
        data.getDailyGemEarningMillis().clear();
    }

    private static int dailyKey(long time) {
        return dailyKey(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate());
    }

    private static int dailyKey(LocalDate date) {
        return date.getYear() * 10_000 + date.getMonthValue() * 100 + date.getDayOfMonth();
    }

    /** 生命周期先完成每日/跨季切换，宝石再按最新赛季状态结算。 */
    @Override
    public int order() {
        return 110;
    }

    /** 一次合成解析出的上下文。 */
    private static final class CraftContext {
        final int quality;
        final SeasonGemCraftCfg craft;
        final SeasonGemCfg first;
        final boolean allSame;
        final Map<Integer, Long> input;

        CraftContext(int quality, SeasonGemCraftCfg craft, SeasonGemCfg first,
                     boolean allSame, Map<Integer, Long> input) {
            this.quality = quality;
            this.craft = craft;
            this.first = first;
            this.allSame = allSame;
            this.input = input;
        }
    }

    private static final class CraftOutcome {
        final SeasonCraftResult result;
        final Map<Integer, Long> requiredItems;
        final Map<Integer, Long> consumedItems;
        final Map<Integer, Long> resultItems;
        final Map<Integer, Long> failKeepItems;

        CraftOutcome(SeasonCraftResult result, Map<Integer, Long> requiredItems,
                     Map<Integer, Long> consumedItems, Map<Integer, Long> resultItems,
                     Map<Integer, Long> failKeepItems) {
            this.result = result;
            this.requiredItems = requiredItems;
            this.consumedItems = consumedItems;
            this.resultItems = resultItems;
            this.failKeepItems = failKeepItems;
        }
    }

    private static final class BatchAccumulator {
        int craftCount;
        int successCount;
        long totalCost;
        final Map<Integer, Long> craftCountsByQuality = new LinkedHashMap<>();
        final Map<Integer, Long> requiredItems = new LinkedHashMap<>();
        final Map<Integer, Long> consumedItems = new LinkedHashMap<>();
        final Map<Integer, Long> resultItems = new LinkedHashMap<>();
        final Map<Integer, Long> failKeepItems = new LinkedHashMap<>();

        void add(CraftOutcome outcome, int coinCost) {
            craftCount++;
            craftCountsByQuality.merge(outcome.result.getMaterialQuality(), 1L, Long::sum);
            if (outcome.result.isSuccess()) {
                successCount++;
            }
            totalCost += coinCost;
            merge(requiredItems, outcome.requiredItems);
            merge(consumedItems, outcome.consumedItems);
            merge(resultItems, outcome.resultItems);
            merge(failKeepItems, outcome.failKeepItems);
        }

        private static void merge(Map<Integer, Long> target, Map<Integer, Long> source) {
            source.forEach((itemId, count) -> target.merge(itemId, count, Long::sum));
        }
    }

    private SeasonGemCfg chooseOutput(SeasonGemCfg first, boolean allSame, List<List<Integer>> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        if (allSame) {
            for (List<Integer> row : rows) {
                if (row == null || row.isEmpty()) continue;
                SeasonGemCfg candidate = configService.gem(row.get(0));
                if (candidate != null && candidate.getType() == first.getType()
                        && candidate.getGenre() == first.getGenre()) {
                    return candidate;
                }
            }
        }
        WeightRandom<Integer> random = WeightRandom.create();
        for (List<Integer> row : rows) {
            if (row != null && row.size() >= 2 && row.get(1) > 0) {
                random.add(row.get(0), row.get(1));
            }
        }
        Integer cfgId = random.next();
        return cfgId == null ? null : configService.gem(cfgId);
    }

    private int outputCount(int cfgId, List<List<Integer>> rows) {
        for (List<Integer> row : rows) {
            if (row != null && row.size() >= 3 && row.get(0) == cfgId) {
                return Math.max(1, row.get(2));
            }
        }
        return 1;
    }

}
