package com.jjg.game.core.base.condition.numeric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 一次游戏结算事件。字段均为本次结算的事实值，不包含玩家历史累计值。
 * <p>
 * slots 节点和 hall 内模拟玩法都可构造此事件；集合在构造时会做只读快照，允许跨方法安全复用。
 */
public record GameConditionEvent(
        int gameId,
        int gameType,
        int roomType,
        int betItemId,
        int winItemId,
        long bet,
        long win,
        long multiple,
        boolean energyConsumed,
        boolean normalSpin,
        int awardType,
        Map<Integer, Long> jackpotCounts,
        int freeGameTriggers,
        int gemDrops,
        Set<Integer> specialModes,
        List<Integer> icons,
        Map<Integer, Long> itemGains) implements ConditionEvent {

    public GameConditionEvent {
        specialModes = specialModes == null ? Set.of() : Set.copyOf(specialModes);
        icons = icons == null ? List.of() : List.copyOf(icons);
        itemGains = itemGains == null ? Map.of() : Map.copyOf(itemGains);
        jackpotCounts = jackpotCounts == null ? Map.of() : Map.copyOf(jackpotCounts);
    }

    /**
     * 本次旋转触发的奖池次数。一次旋转可同时命中多个档位，各档独立计数，不互相遮蔽。
     *
     * @param tier 奖池档位 (1.MINI 2.MINOR 3.MAJOR 4.GRAND); <=0 表示不限档位，取各档合计
     */
    public long jackpotCount(int tier) {
        if (tier > 0) {
            return jackpotCounts.getOrDefault(tier, 0L);
        }
        long total = 0;
        for (Long count : jackpotCounts.values()) {
            total += count;
        }
        return total;
    }

    public boolean matchesGame(long configuredGameId) {
        return configuredGameId <= 0 || configuredGameId == gameId || configuredGameId == gameType;
    }

    public boolean containsMode(int modeId) {
        return specialModes.contains(modeId);
    }

    public long iconCount(int iconId) {
        long count = 0;
        for (Integer icon : icons) {
            if (icon != null && icon == iconId) {
                count++;
            }
        }
        return count;
    }

    public long itemGain(int itemId) {
        return itemGains.getOrDefault(itemId, 0L);
    }

    /**
     * 赛季宝石掉落在本事件构造之后才结算，就地派生一个带掉落事实的新事件（一次旋转最多掉落一次）。
     * 宝石同样是本次到账的道具，一并计入收益明细；无掉落时返回自身，热路径不产生额外对象。
     */
    public GameConditionEvent withGemDrop(Map<Integer, Long> gemGains) {
        if (gemGains == null || gemGains.isEmpty()) {
            return this;
        }
        Map<Integer, Long> merged = new HashMap<>(itemGains);
        for (Map.Entry<Integer, Long> en : gemGains.entrySet()) {
            if (en.getKey() != null && en.getValue() != null && en.getValue() > 0) {
                merged.merge(en.getKey(), en.getValue(), Long::sum);
            }
        }
        return new GameConditionEvent(gameId, gameType, roomType, betItemId, winItemId,
                bet, win, multiple, energyConsumed, normalSpin, awardType, jackpotCounts,
                freeGameTriggers, gemDrops + 1, specialModes, icons, merged);
    }

    /**
     * 优先返回明确上报的道具收益；未拆分收益明细时，使用本局 win 兼容金币等单币种游戏。
     */
    public long winOf(int itemId) {
        long itemValue = itemGain(itemId);
        return itemValue > 0 ? itemValue : (itemId <= 0 || itemId == winItemId ? win : 0);
    }
}
