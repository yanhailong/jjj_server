package com.jjg.game.season.service;

import java.util.List;

/**
 * 赛季区间和万分比配置的纯计算方法。
 */
public final class SeasonPolicy {
    public static final int RATIO_BASE = 10_000;

    private SeasonPolicy() {
    }

    /**
     * 查找左闭右开区间对应的万分比；没有命中时不削减。
     */
    public static int ratioFor(long value, List<List<Integer>> ranges) {
        if (ranges == null) {
            return RATIO_BASE;
        }
        for (List<Integer> row : ranges) {
            if (row == null || row.size() < 3) {
                continue;
            }
            if (value >= row.get(0) && value < row.get(1)) {
                return Math.max(0, row.get(2));
            }
        }
        return RATIO_BASE;
    }

    public static long applyRatio(long value, int ratio) {
        if (value <= 0 || ratio <= 0) {
            return 0;
        }
        return Math.multiplyExact(value, Math.min(ratio, RATIO_BASE)) / RATIO_BASE;
    }

    public static boolean contains(List<Integer> range, long value) {
        if (range == null || range.isEmpty()) {
            return false;
        }
        long min = range.get(0);
        long max = range.size() > 1 ? range.get(1) : min;
        return value >= min && value <= max;
    }
}
