package com.jjg.game.ploy.games.highlowpoker.data;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2026/4/2 09:21
 */
public record HighLowParam(
        //返奖率
        BigDecimal returnRate,
        //剩余数量
        BigDecimal remainCount,
        //当前点数
        int currentPoint,
        //当前数量
        int num) {
    public HighLowParam(BigDecimal returnRate, BigDecimal remainCount, int num) {
        this(returnRate, remainCount, 0, num);
    }
}
