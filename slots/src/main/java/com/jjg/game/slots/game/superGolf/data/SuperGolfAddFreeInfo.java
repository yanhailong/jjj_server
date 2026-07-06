package com.jjg.game.slots.game.superGolf.data;

import java.util.HashMap;
import java.util.Map;

/**
 * 增加免费次数配置（SpecialPlay 表 playType=2）。
 * <p>
 * 超级高尔夫文档：4 scatter→10 局，5→12，6→14，每多 1 个 scatter +2 局。
 * 配表格式：{@code libType,targetIcon,count_addFreeCount_prop|...}<br>
 * 示例：{@code 2,113,4_10_10000|5_12_10000|6_14_10000}
 */
public class SuperGolfAddFreeInfo {
    private int libType;
    private int targetIcon;
    private final Map<Integer, Integer> countToAddFree = new HashMap<>();
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

    /** 按盘面 scatter 实际个数查应得免费次数；未精确配的档位向下取最大可用档 */
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
