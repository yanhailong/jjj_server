package com.jjg.game.season.service;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.SeasonGemCfg;
import com.jjg.game.sampledata.bean.SeasonGemCraftCfg;
import com.jjg.game.sampledata.bean.SeasonStartCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.season.data.SeasonCraftResult;
import com.jjg.game.season.data.SeasonPendingCraft;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.data.SeasonSlotsSessionData;
import com.jjg.game.sim.service.SimAutoSaveService;
import com.jjg.game.sim.service.SimPackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
        int currentItemId = data.getEquippedGems().getOrDefault(slot, 0);
        if (itemId == 0) {
            if (currentItemId <= 0) {
                return new CommonResult<>(Code.SUCCESS, Map.copyOf(data.getEquippedGems()));
            }
            CommonResult<?> restored = simPackService.addItems(ctx, Map.of(currentItemId, 1L),
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
        if (!simPackService.removeItems(ctx, Map.of(itemId, 1L), AddType.ITEM_EXCHANGE, "season-gem-equip")) {
            log.warn("赛季宝石镶嵌失败,道具不足 playerId={},itemId={}", ctx.playerId(), itemId);
            return new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        }
        if (currentItemId > 0) {
            CommonResult<?> restored = simPackService.addItems(ctx, Map.of(currentItemId, 1L),
                    AddType.ITEM_EXCHANGE, "season-gem-unequip", true);
            if (!restored.success()) {
                simPackService.addItems(ctx, Map.of(itemId, 1L), AddType.FAIL_ROLLBACK, "season-gem-equip", true);
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
     * 因此概率权重按槽位逐个累加；下注额属于“解锁”语义，只保留去重后的并集。</p>
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
                    if (cfg.getBet() != null) {
                        cfg.getBet().stream().filter(Objects::nonNull).forEach(unlockedBets::add);
                    }
                    mergeDelta(libTypeWeightDelta, cfg.getSpecialMode());
                    mergeSectionDelta(sectionWeightDelta, cfg.getWinRate());
                    mergeSectionDelta(sectionWeightDelta, cfg.getSpecialModeProbUp());
                });

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
     * 合成第一步: 发起合成。
     *
     * <p>校验材料与赛季币后掷点，赛季币与全部材料在此立即扣除(无论成败)：赛季币锁定本次掷点、
     * 避免失败后重复发起免费重掷；材料以"托管"方式先行扣除、避免两步之间被消耗或转移绕过失败消耗。
     * 材料仅来自背包未镶嵌宝石。掷点成功则直接产出宝石；掷点失败则记入待结算态({@link SeasonPendingCraft})，
     * 返回 success=false，等待玩家第二步通过 {@link #craftKeep} 选择保留的宝石
     * (掉线/重登则由 {@link #autoSettleFailedCraft} 默认保留第一件)。</p>
     */
    public CommonResult<SeasonCraftResult> craft(SimPlayerContext ctx, List<Integer> itemIds) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        //存在未结算的失败合成时，必须先完成第二步选择，禁止重复发起
        if (data.getPendingCraft() != null) {
            log.warn("赛季宝石合成存在未完成的失败合成 playerId={}", ctx.playerId());
            return new CommonResult<>(Code.REPEAT_OP);
        }
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

        boolean success = successRoll.test(craft.getMergeSuccessRate());
        //赛季币与全部材料在发起时即扣除(成功/失败一致): 赛季币防重复掷取, 材料托管防两步之间绕过失败消耗
        data.setSeasonCoin(data.getSeasonCoin() - craft.getMergeCost());
        Map<Integer, Long> consumed = new HashMap<>(craftCtx.input);
        if (!simPackService.removeItems(ctx, consumed, AddType.ITEM_EXCHANGE, "season-gem-craft")) {
            data.setSeasonCoin(data.getSeasonCoin() + craft.getMergeCost());
            log.warn("赛季宝石合成扣除材料失败 playerId={},quality={}", ctx.playerId(), craftCtx.quality);
            return new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        }

        SeasonCraftResult result = new SeasonCraftResult();
        result.setSuccess(success);
        if (!success) {
            //失败: 材料已托管扣除, 记入待结算态, 等待第二步(或掉线/重登)选择保留的宝石后返还
            data.setPendingCraft(new SeasonPendingCraft(new ArrayList<>(itemIds), craft.getFailKeepAmount()));
            autoSaveService.enqueueSave(data);
            return new CommonResult<>(Code.SUCCESS, result);
        }

        //成功: 材料已扣除, 产出宝石(产出异常则整体回滚材料与赛季币)
        SeasonGemCfg output = chooseOutput(craftCtx.first, craftCtx.allSame, craft.getSuccessGem());
        if (output == null) {
            rollback(ctx, data, consumed, craft.getMergeCost());
            log.warn("赛季宝石合成产出配置错误 playerId={},quality={}", ctx.playerId(), craftCtx.quality);
            return new CommonResult<>(Code.NOT_FOUND);
        }
        int count = outputCount(output.getId(), craft.getSuccessGem());
        Map<Integer, Long> reward = Map.of(output.getItemId(), (long) count);
        CommonResult<?> add = simPackService.addItems(ctx, reward, AddType.ITEM_EXCHANGE,
                "season-gem-craft", true);
        if (!add.success()) {
            rollback(ctx, data, consumed, craft.getMergeCost());
            log.warn("赛季宝石合成产出入账失败 playerId={},quality={},code={}",
                    ctx.playerId(), craftCtx.quality, add.code);
            return new CommonResult<>(add.code);
        }
        result.setResultItemId(output.getItemId());
        result.setResultCount(count);
        autoSaveService.enqueueSave(data);
        return new CommonResult<>(Code.SUCCESS, result);
    }

    /**
     * 合成第二步: 合成失败后选择保留的宝石并结算。
     *
     * <p>材料已在第一步托管扣除，此处只把玩家选择保留的宝石返还背包(赛季币不再扣除)。
     * 返还成功返回实际保留的宝石道具ID；返还失败则保留待结算态并把错误码返回客户端供重试。</p>
     */
    public CommonResult<Integer> craftKeep(SimPlayerContext ctx, int keepItemId) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        SeasonPendingCraft pending = data.getPendingCraft();
        if (pending == null) {
            log.warn("赛季宝石合成无待结算失败记录 playerId={}", ctx.playerId());
            return new CommonResult<>(Code.NOT_FOUND);
        }
        if (!pending.getItemIds().contains(keepItemId)) {
            log.warn("赛季宝石合成保留道具无效 playerId={},keepItemId={}", ctx.playerId(), keepItemId);
            return new CommonResult<>(Code.PARAM_ERROR);
        }
        int code = settleFailedCraft(ctx, data, pending, keepItemId);
        if (code != Code.SUCCESS) {
            return new CommonResult<>(code);
        }
        return new CommonResult<>(Code.SUCCESS, keepItemId);
    }

    /**
     * 自动结算未完成的失败合成: 默认保留材料中的第一件。
     * 用于玩家掉线/登出({@code onExitGame})、以及进程异常/崩溃后玩家重登时补偿({@code createContextByPlayerId})。
     * 返还失败时保留待结算态(已落库)，待下次重登再重试，避免永久损失应保留的宝石。
     */
    public void autoSettleFailedCraft(SimPlayerContext ctx) {
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        if (data == null) {
            return;
        }
        SeasonPendingCraft pending = data.getPendingCraft();
        if (pending == null || pending.getItemIds().isEmpty()) {
            return;
        }
        int keepItemId = pending.getItemIds().getFirst();
        int code = settleFailedCraft(ctx, data, pending, keepItemId);
        if (code == Code.SUCCESS) {
            log.info("自动结算失败合成, 默认保留第一件 playerId={},keepItemId={}", ctx.playerId(), keepItemId);
        } else {
            log.warn("自动结算失败合成未成功, 待结算态保留待重登重试 playerId={},keepItemId={},code={}",
                    ctx.playerId(), keepItemId, code);
        }
    }

    /**
     * 结算一次失败合成: 把玩家保留的宝石按 keepAmount 返还背包(其余材料已在第一步托管扣除，不再重复扣除)。
     * 返还量按本次材料中该宝石的持有量截断。
     * <p>仅在返还成功后清除待结算态；返还失败(如背包锁/存储异常)则保留待结算态供后续重试，避免玩家永久损失应保留的宝石。</p>
     *
     * @return 结算结果码；{@link Code#SUCCESS} 表示已返还并清除待结算态，其它为返还失败对应的错误码。
     */
    private int settleFailedCraft(SimPlayerContext ctx, SeasonPlayerData data,
                                  SeasonPendingCraft pending, int keepItemId) {
        List<Integer> materials = pending.getItemIds();
        int held = Collections.frequency(materials, keepItemId);
        long keepCount = Math.min(pending.getKeepAmount(), held);
        if (keepCount > 0) {
            CommonResult<?> add = simPackService.addItems(ctx, Map.of(keepItemId, keepCount),
                    AddType.ITEM_EXCHANGE, "season-gem-craft", true);
            if (!add.success()) {
                log.warn("赛季宝石合成失败返还宝石失败, 保留待结算态待重试 playerId={},keepItemId={},keepCount={},code={}",
                        ctx.playerId(), keepItemId, keepCount, add.code);
                return add.code;
            }
        }
        data.setPendingCraft(null);
        autoSaveService.enqueueSave(data);
        return Code.SUCCESS;
    }

    /**
     * 校验合成材料并解析出合成上下文(材料构成、配置、品质等)，不含赛季币校验。仅第一步发起合成使用。
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
        if (!playerPackService.checkHasItems(ctx.getPlayerController().getPlayer(), input)) {
            log.warn("赛季宝石合成道具不足 playerId={},quality={},count={}",
                    ctx.playerId(), quality, itemIds.size());
            return new CommonResult<>(Code.NOT_ENOUGH_ITEM);
        }
        return new CommonResult<>(Code.SUCCESS, new CraftContext(quality, craft, first, allSame, input));
    }

    /**
     * 一次合成解析出的上下文，供发起与保留两步复用。
     */
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
