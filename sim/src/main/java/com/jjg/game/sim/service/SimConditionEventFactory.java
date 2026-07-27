package com.jjg.game.sim.service;

import com.jjg.game.core.base.condition.numeric.ActionConditionEvent;
import com.jjg.game.core.base.condition.numeric.GameConditionEvent;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sim.data.SpinStatInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * slots 跨节点旋转统计到 core 条件事件的唯一转换边界。
 * <p>
 * 同一 spin 只创建一个不可变事件，供 sim 任务、联盟任务和赛季试炼同步复用，避免高频路径
 * 重复复制模式/图标集合。转换不访问数据库、Redis，也不切换线程。
 */
public final class SimConditionEventFactory {
    private static final Logger log = LoggerFactory.getLogger(SimConditionEventFactory.class);
    private static final AtomicBoolean missingGoldLogged = new AtomicBoolean();
    private static volatile int goldItemId;

    private SimConditionEventFactory() {
    }

    public static GameConditionEvent fromSpin(int gameType, int winTimes, int costPower,
                                              SpinStatInfo statInfo) {
        return fromSpin(gameType, winTimes, costPower, statInfo, resolveGoldItemId(), null);
    }

    /**
     * 本次旋转另有道具产出 (掉落等) 时传入明细, 供 12202 等按道具 id 计数的条件推进。
     * itemGains 必须是本次旋转真实到账的道具, 掉什么报什么, 新增道具无需改动本方法。
     */
    public static GameConditionEvent fromSpin(int gameType, int winTimes, int costPower,
                                              SpinStatInfo statInfo, Map<Integer, Long> itemGains) {
        return fromSpin(gameType, winTimes, costPower, statInfo, resolveGoldItemId(), itemGains);
    }

    /** 测试/无货币条件调用可显式传 0，避免依赖尚未初始化的 Item 配置。 */
    public static GameConditionEvent fromSpin(int gameType, int winTimes, int costPower,
                                              SpinStatInfo statInfo, int goldItemId) {
        return fromSpin(gameType, winTimes, costPower, statInfo, goldItemId, null);
    }

    private static GameConditionEvent fromSpin(int gameType, int winTimes, int costPower,
                                               SpinStatInfo statInfo, int goldItemId,
                                               Map<Integer, Long> itemGains) {
        long bet = statInfo == null ? costPower : statInfo.getBet();
        long win = statInfo == null ? 0 : statInfo.getWin();
        return new GameConditionEvent(gameType, gameType, 0, goldItemId, goldItemId,
                bet, win, winTimes, costPower > 0 || bet > 0, true,
                statInfo == null ? 0 : statInfo.getBigShowId(),
                statInfo == null ? Map.of() : statInfo.getJackpotCounts(),
                statInfo != null && statInfo.getRemainFreeCount() > 0 ? 1 : 0,
                //赛季宝石掉落在 SeasonService 结算后由 withGemDrop 补入
                0,
                statInfo == null || statInfo.getSpecialModes() == null
                        ? Set.of() : Set.copyOf(statInfo.getSpecialModes()),
                statInfo == null ? List.of() : statInfo.getIcons(),
                mergeGains(goldItemId, win, itemGains));
    }

    /** 本局收益明细: 金币赢奖 + 本次旋转的道具产出, 空值与非正数安全 (record 构造会拒绝 null)。 */
    private static Map<Integer, Long> mergeGains(int goldItemId, long win, Map<Integer, Long> itemGains) {
        if (itemGains == null || itemGains.isEmpty()) {
            return win > 0 ? Map.of(goldItemId, win) : Map.of();
        }
        Map<Integer, Long> merged = new HashMap<>(itemGains.size() + 1);
        for (Map.Entry<Integer, Long> en : itemGains.entrySet()) {
            if (en.getKey() != null && en.getValue() != null && en.getValue() > 0) {
                merged.merge(en.getKey(), en.getValue(), Long::sum);
            }
        }
        if (win > 0) {
            merged.merge(goldItemId, win, Long::sum);
        }
        return merged;
    }

    /**
     * 非 spin 游戏结算和旧跨节点协议的统一适配入口，同样使用金币配置未就绪时的安全降级策略。
     */
    public static GameConditionEvent fromGameResult(int gameType, long bet, long win, long multiple) {
        int goldItemId = resolveGoldItemId();
        return new GameConditionEvent(gameType, gameType, 0, goldItemId, goldItemId,
                bet, win, multiple, true, true, 0, Map.of(), 0, 0,
                Set.of(), List.of(), win > 0 ? Map.of(goldItemId, win) : Map.of());
    }

    // =====================================================================
    // 经营类动作事件 (sim 主线 12208-12220 使用; 均为本次动作的事实值, 不含历史累计)
    // =====================================================================

    /** 登陆一次 (按自然日去重由调用方保证): 推进 12218 累积登陆天数, 每次记 1。 */
    public static ActionConditionEvent login() {
        return new ActionConditionEvent(ActionConditionEvent.Type.LOGIN, 0, 0, 0, 1, 0, false);
    }

    /** 观看广告一次: 推进 12209 观看广告次数。 */
    public static ActionConditionEvent adWatch() {
        return new ActionConditionEvent(ActionConditionEvent.Type.AD_WATCH, 0, 0, 0, 1, 0, false);
    }

    /**
     * 当前已研发的游戏总数 (研究院等级达标的游戏并集): 推进 12216 累积研发游戏数。
     * 上报的是总数而非增量, 条件按 SET 覆盖进度, 重复上报幂等。
     */
    public static ActionConditionEvent gameResearched(long count) {
        return new ActionConditionEvent(ActionConditionEvent.Type.GAME_UNLOCK,
                0, 0, Math.max(0, count), 0, 0, false);
    }

    /** 拜访一次: 推进 12217 累积拜访次数。 */
    public static ActionConditionEvent visit() {
        return new ActionConditionEvent(ActionConditionEvent.Type.VISIT, 0, 0, 0, 1, 0, false);
    }

    /**
     * 一次经营金币收益 (自产/离线/游客产出): 推进 12215 经营收益累积。
     * subject 取金币道具 id, 供 12215 的道具过滤维度匹配 (金币配置未就绪时退化为 0, 本次不推进)。
     */
    public static ActionConditionEvent businessIncome(long gold) {
        return new ActionConditionEvent(ActionConditionEvent.Type.PRODUCTION_INCOME,
                resolveGoldItemId(), 0, Math.max(0, gold), 0, 0, false);
    }

    /** 一次道具消费: 推进 12220 累积消费 (按 itemId 过滤金币/钻石等)。 */
    public static ActionConditionEvent itemConsume(int itemId, long count) {
        return new ActionConditionEvent(ActionConditionEvent.Type.ITEM_CONSUME,
                itemId, 0, Math.max(0, count), 0, 0, false);
    }

    /**
     * 生成"拥有型"计数条件的事件序列 (12208 建筑 / 12212 雇员 / 12214 游客)。
     * <p>
     * 这三条规则语义为 SET + {@code qualifier(等级/星级) >= 配置阈值} 时把进度覆盖为事件 value。
     * 为使"持有量 ≥ 任意阈值 T"都能被正确覆盖: 把当前持有量按 tier 分档, 由高到低对每个存在的 tier
     * 发一个事件, value = 该 tier 及以上的累计持有量, qualifier = 该 tier。这样对任意阈值 T, 匹配到的
     * 最后一个事件恰是"存在的、≥T 的最小 tier", 其累计量即 count(≥T), SET 结果正确; 无任何 tier ≥T
     * 时不发匹配事件, 进度保持不变。
     *
     * @param sink        事件下发口 (通常为 {@code e -> simTaskService.onConditionEvent(ctx, e)})
     * @param type        条件事件类型 (BUILDING_COUNT / EMPLOYEE_COUNT / GUEST_COUNT)
     * @param subjectId   主体过滤维度 (0 表示无过滤; 建筑/游客传 0, 雇员传职业 id)
     * @param countByTier tier(等级/星级) -> 恰好处于该 tier 的持有数量
     */
    public static void emitOwnershipCounts(Consumer<ActionConditionEvent> sink,
                                           ActionConditionEvent.Type type, int subjectId,
                                           SortedMap<Integer, Long> countByTier) {
        if (sink == null || countByTier == null || countByTier.isEmpty()) {
            return;
        }
        long cumulative = 0;
        //从最高 tier 向下累计, cumulative 即"≥当前 tier 的持有量"
        for (Integer tier : new java.util.TreeSet<>(countByTier.keySet()).descendingSet()) {
            Long tierCount = countByTier.get(tier);
            if (tierCount == null || tierCount <= 0) {
                continue;
            }
            cumulative += tierCount;
            sink.accept(new ActionConditionEvent(type, subjectId, 0, cumulative, 0, tier, false));
        }
    }

    private static int resolveGoldItemId() {
        int cached = goldItemId;
        if (cached > 0) {
            return cached;
        }
        try {
            cached = ItemUtils.getGoldItemId();
            if (cached > 0) {
                goldItemId = cached;
                return cached;
            }
        } catch (RuntimeException e) {
            if (missingGoldLogged.compareAndSet(false, true)) {
                log.error("构造条件事件时金币道具配置尚未就绪，货币过滤条件本次不推进", e);
            }
        }
        return 0;
    }
}
