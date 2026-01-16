package com.jjg.game.core.base.condition;

import com.jjg.game.core.constant.Code;

import java.math.BigDecimal;

/**
 * @author lm
 * @date 2026/1/14 13:34
 */
public record MatchResultData(
        MatchResult result,
        int achieveTimes,
        int errorCode,
        BigDecimal need,
        BigDecimal current
) {
    public static MatchResultData match() {
        return new MatchResultData(MatchResult.MATCH, 1, Code.SUCCESS, null, null);
    }

    public static MatchResultData notMatch(int code) {
        return new MatchResultData(MatchResult.NOT_MATCH, 1, code, null, null);

    }

    public static MatchResultData notMatch(int code, long need, long current) {
        return new MatchResultData(MatchResult.NOT_MATCH, 1, code, BigDecimal.valueOf(need), BigDecimal.valueOf(current));
    }

    public static MatchResultData notMatch(int code, BigDecimal need, BigDecimal current) {
        return new MatchResultData(MatchResult.NOT_MATCH, 1, code, need, current);
    }

    public static MatchResultData unknown() {
        return new MatchResultData(MatchResult.UNKNOWN, 0, 0, null, null);
    }
}
