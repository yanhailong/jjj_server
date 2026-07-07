package com.jjg.game.sim.constant;

/**
 * 拜访系统业务常量。
 *
 * @author 11
 * @date 2026/6/30
 */
public interface SimVisitConstant {
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
