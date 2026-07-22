package com.jjg.game.season.service;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.bean.SeasonGemCfg;
import com.jjg.game.sampledata.bean.SeasonGemCraftCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.season.data.SeasonCraftResult;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.sim.service.SimAutoSaveService;
import com.jjg.game.sim.service.SimPackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/**
 * 宝石镶嵌与配置驱动的合成逻辑。
 */
@Service
public class SeasonGemService {
    private static final Logger log = LoggerFactory.getLogger(SeasonGemService.class);
    private static final int SLOTS_PER_TYPE = 3;
    private static final int GEM_TYPE_COUNT = 3;

    private final SeasonConfigService configService;
    private final SimPackService simPackService;
    private final PlayerPackService playerPackService;
    private final SimAutoSaveService autoSaveService;
    private final IntPredicate successRoll;

    @Autowired
    public SeasonGemService(SeasonConfigService configService, SimPackService simPackService,
                            PlayerPackService playerPackService, SimAutoSaveService autoSaveService) {
        this(configService, simPackService, playerPackService, autoSaveService,
                RandomUtils::getRandomBoolean100);
    }

    public SeasonGemService(SeasonConfigService configService, SimPackService simPackService,
                            PlayerPackService playerPackService, SimAutoSaveService autoSaveService,
                            IntPredicate successRoll) {
        this.configService = configService;
        this.simPackService = simPackService;
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
        if (itemId == 0) {
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
        long equippedCopies = data.getEquippedGems().values().stream()
                .filter(equippedItemId -> equippedItemId == itemId)
                .count();
        boolean alreadyInSlot = data.getEquippedGems().getOrDefault(slot, 0) == itemId;
        long requiredCopies = equippedCopies + (alreadyInSlot ? 0 : 1);
        if (!playerPackService.checkHasItems(ctx.getPlayerController().getPlayer(),
                Map.of(itemId, requiredCopies))) {
            log.warn("赛季宝石镶嵌失败,道具不足 playerId={},itemId={}", ctx.playerId(), itemId);
            return new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        }
        data.getEquippedGems().put(slot, itemId);
        autoSaveService.enqueueSave(data);
        return new CommonResult<>(Code.SUCCESS, Map.copyOf(data.getEquippedGems()));
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

    public CommonResult<SeasonCraftResult> craft(SimPlayerContext ctx, List<Integer> itemIds, int keepItemId) {
        if (itemIds == null || itemIds.isEmpty()) {
            log.warn("赛季宝石合成参数为空 playerId={}", ctx.playerId());
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        SeasonGemCfg first = configService.gemByItemId(itemIds.getFirst());
        if (first == null) {
            log.warn("赛季宝石合成配置不存在 playerId={},itemId={}", ctx.playerId(), itemIds.getFirst());
            return new CommonResult<>(Code.NOT_FOUND);
        }
        SeasonGemCraftCfg craft = configService.craftForRarity(first.getRarity());
        if (craft == null || itemIds.size() != craft.getCostAmount()) {
            log.warn("赛季宝石合成数量或配置错误 playerId={},rarity={},count={}",
                    ctx.playerId(), first.getRarity(), itemIds.size());
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        Map<Integer, Long> input = new HashMap<>();
        boolean allSame = true;
        for (int itemId : itemIds) {
            SeasonGemCfg gem = configService.gemByItemId(itemId);
            if (gem == null || gem.getRarity() != first.getRarity()) {
                log.warn("赛季宝石合成品质不一致 playerId={},rarity={},count={}",
                        ctx.playerId(), first.getRarity(), itemIds.size());
                return new CommonResult<>(Code.PARAM_ERROR);
            }
            allSame &= itemId == first.getItemId();
            input.merge(itemId, 1L, Long::sum);
        }
        if (!playerPackService.checkHasItems(ctx.getPlayerController().getPlayer(), input)) {
            log.warn("赛季宝石合成道具不足 playerId={},rarity={},count={}",
                    ctx.playerId(), first.getRarity(), itemIds.size());
            return new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        }
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (data.getSeasonCoin() < craft.getMergeCost()) {
            log.warn("赛季宝石合成赛季币不足 playerId={},need={},have={}",
                    ctx.playerId(), craft.getMergeCost(), data.getSeasonCoin());
            return new CommonResult<>(Code.NOT_ENOUGH);
        }

        boolean success = successRoll.test(craft.getMergeSuccessRate());
        Map<Integer, Long> consumed = new HashMap<>(input);
        if (!success) {
            if (!input.containsKey(keepItemId)) {
                log.warn("赛季宝石合成保留道具无效 playerId={},keepItemId={}", ctx.playerId(), keepItemId);
                return new CommonResult<>(Code.PARAM_ERROR);
            }
            consumed.computeIfPresent(keepItemId,
                    (ignored, count) -> Math.max(0, count - craft.getFailKeepAmount()));
            consumed.values().removeIf(value -> value <= 0);
        }
        if (!simPackService.removeItems(ctx, consumed, AddType.ITEM_EXCHANGE, "season-gem-craft")) {
            log.warn("赛季宝石合成扣除失败 playerId={},rarity={}", ctx.playerId(), first.getRarity());
            return new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        }
        data.setSeasonCoin(data.getSeasonCoin() - craft.getMergeCost());

        SeasonCraftResult result = new SeasonCraftResult();
        result.setSuccess(success);
        if (success) {
            SeasonGemCfg output = chooseOutput(first, allSame, craft.getSuccessGem());
            if (output == null) {
                rollback(ctx, data, consumed, craft.getMergeCost());
                log.warn("赛季宝石合成产出配置错误 playerId={},rarity={}", ctx.playerId(), first.getRarity());
                return new CommonResult<>(Code.NOT_FOUND);
            }
            int count = outputCount(output.getId(), craft.getSuccessGem());
            Map<Integer, Long> reward = Map.of(output.getItemId(), (long) count);
            CommonResult<?> add = simPackService.addItems(ctx, reward, AddType.ITEM_EXCHANGE,
                    "season-gem-craft", true);
            if (!add.success()) {
                rollback(ctx, data, consumed, craft.getMergeCost());
                log.warn("赛季宝石合成产出入账失败 playerId={},rarity={},code={}",
                        ctx.playerId(), first.getRarity(), add.code);
                return new CommonResult<>(add.code);
            }
            result.setResultItemId(output.getItemId());
            result.setResultCount(count);
        } else {
            result.setKeptItemId(keepItemId);
        }
        unequipExcess(ctx, data, input.keySet());
        autoSaveService.enqueueSave(data);
        return new CommonResult<>(Code.SUCCESS, result);
    }

    /**
     * 合成消耗后按背包剩余数量复核镶嵌: 只卸下超出剩余持有量的镶嵌位, 剩余数量足够的保持不动。
     */
    private void unequipExcess(SimPlayerContext ctx, SeasonPlayerData data, Collection<Integer> itemIds) {
        if (itemIds.isEmpty() || data.getEquippedGems().isEmpty()) {
            return;
        }
        PlayerPack pack = playerPackService.getFromAllDB(ctx.playerId());
        for (Integer itemId : itemIds) {
            long remaining = pack == null ? 0 : pack.getItemCount(itemId);
            Iterator<Map.Entry<Integer, Integer>> it = data.getEquippedGems().entrySet().iterator();
            while (it.hasNext()) {
                if (!it.next().getValue().equals(itemId)) {
                    continue;
                }
                if (remaining > 0) {
                    remaining--;
                } else {
                    it.remove();
                }
            }
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

    private void rollback(SimPlayerContext ctx, SeasonPlayerData data, Map<Integer, Long> consumed, int coin) {
        data.setSeasonCoin(data.getSeasonCoin() + coin);
        if (!consumed.isEmpty()) {
            simPackService.addItems(ctx, consumed, AddType.FAIL_ROLLBACK, "season-gem-craft", true);
        }
    }
}
