package com.jjg.game.core.base.condition.numeric;

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
        int jackpotType,
        int freeGameTriggers,
        Set<Integer> specialModes,
        List<Integer> icons,
        Map<Integer, Long> itemGains) implements ConditionEvent {

    public GameConditionEvent {
        specialModes = specialModes == null ? Set.of() : Set.copyOf(specialModes);
        icons = icons == null ? List.of() : List.copyOf(icons);
        itemGains = itemGains == null ? Map.of() : Map.copyOf(itemGains);
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
     * 优先返回明确上报的道具收益；未拆分收益明细时，使用本局 win 兼容金币等单币种游戏。
     */
    public long winOf(int itemId) {
        long itemValue = itemGain(itemId);
        return itemValue > 0 ? itemValue : (itemId <= 0 || itemId == winItemId ? win : 0);
    }
}
