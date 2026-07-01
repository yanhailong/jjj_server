package com.jjg.game.sim.constant;

/**
 * 拜访系统业务常量。
 *
 * @author 11
 * @date 2026/6/30
 */
public interface SimVisitConstant {
    interface Reason {
        int NONE = 0;
        int SELF_VISIT = 1;
        int PLAYER_NOT_FOUND = 2;
        int CASINO_NOT_FOUND = 3;
        int DAILY_LIMIT = 4;
        int POPULARITY_LIMIT = 5;
        int COMMENT_INVALID = 6;
        int COMMENT_LOCKED = 7;
        int GIFT_NOT_FOUND = 8;
        int DIAMOND_NOT_ENOUGH = 9;
        int GAME_NOT_UNLOCKED = 10;
        int TRIAL_INVALID = 11;
        int POWER_NOT_ENOUGH = 12;
        int INTERNAL_ERROR = 13;
    }

    interface RecordType {
        int LIKE = 1;
        int COMMENT = 2;
        int GIFT = 3;
        int TRIAL = 4;
    }

    interface QuotaType {
        String LIKE = "like";
        String COMMENT = "comment";
        String TRIAL = "trial";
        String POPULARITY = "popularity";
        String COMMISSION = "commission";
    }
}
