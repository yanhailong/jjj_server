package com.jjg.game.slots.game.dracula.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 增加免费次数配置（SpecialPlay 表 playType=2）
 * <p>
 * 格式：{@code libType,targetIcon,count_addFreeCount_prop|count_addFreeCount_prop|...}<br>
 * 例如 {@code 2,13,4_12_10000|5_14_10000|6_16_10000} 表示：
 * 在 libType=2（免费模式）下，目标符号 id=13（scatter），盘面上出现 4/5/6 个 scatter
 * 分别触发 12/14/16 次免费游戏，万分比 10000（即必中）。
 */
public class DraculaAddFreeInfo {
    /** 触发模式 id（通常为 FREE=2） */
    private int libType;
    /** 触发图标 id（scatter） */
    private int targetIcon;
    /** scatter 个数 → 增加免费次数 */
    private final Map<Integer, Integer> countToAddFree = new HashMap<>();
    /** scatter 个数 → 触发万分比 */
    private final Map<Integer, Integer> countToProp = new HashMap<>();

    public int getLibType() {
        return libType;
    }

    public void setLibType(int libType) {
        this.libType = libType;
    }

    public int getTargetIcon() {
        return targetIcon;
    }

    public void setTargetIcon(int targetIcon) {
        this.targetIcon = targetIcon;
    }

    public void put(int scatterCount, int addFree, int prop) {
        countToAddFree.put(scatterCount, addFree);
        countToProp.put(scatterCount, prop);
    }

    /** 按盘面上 scatter 实际个数查应得的免费次数；找不到精确档则向下取最大可用档 */
    public int getAddFreeCount(int scatterCount) {
        Integer v = countToAddFree.get(scatterCount);
        if (v != null) return v;
        int best = -1;
        int bestKey = -1;
        for (Map.Entry<Integer, Integer> e : countToAddFree.entrySet()) {
            if (e.getKey() <= scatterCount && e.getKey() > bestKey) {
                bestKey = e.getKey();
                best = e.getValue();
            }
        }
        return Math.max(best, 0);
    }

    public int getProp(int scatterCount) {
        return countToProp.getOrDefault(scatterCount, 0);
    }

    public Map<Integer, Integer> getCountToAddFree() {
        return countToAddFree;
    }
}
