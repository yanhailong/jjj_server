package com.jjg.game.sim.service;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sim.constant.SimConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 拜访系统全局配置缓存，初始化和 global.xlsx 热更共用同一加载入口。
 *
 * @author 11
 * @date 2026/6/30
 */
@Service
public class SimVisitConfigService implements ConfigExcelChangeListener {
    private static final Logger log = LoggerFactory.getLogger(SimVisitConfigService.class);
    private static final int MAX_COMMENT_LENGTH = 30;
    private static final int MAX_RECORD_LIMIT = 100;

    private volatile int likePopularity = 2;
    private volatile int commentPopularity = 2;
    private volatile int trialPopularity = 1;
    private volatile int dailyPopularityLimit = 200;
    private volatile int dailyTrialLimit = 100;
    private volatile int commissionRate = 3;
    private volatile int dailyLikeLimit = 10;
    private volatile int dailyCommentLimit = 10;
    private volatile BigDecimal commentRechargeAmount = BigDecimal.ONE;
    private volatile int commentMaxLength = 30;
    private volatile int recordLimit = 100;
    private volatile long dailyCommissionLimit = 1_000_000;
    private volatile int trialSessionSeconds = 1800;

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::load);
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::load);
    }

    public void load() {
        likePopularity = positiveInt(SimConstant.Common.VISIT_LIKE_POPULARITY_ID, 2);
        commentPopularity = positiveInt(SimConstant.Common.VISIT_COMMENT_POPULARITY_ID, 2);
        trialPopularity = positiveInt(SimConstant.Common.VISIT_TRIAL_POPULARITY_ID, 1);
        dailyPopularityLimit = positiveInt(SimConstant.Common.VISIT_DAILY_POPULARITY_LIMIT_ID, 200);
        dailyTrialLimit = positiveInt(SimConstant.Common.VISIT_DAILY_TRIAL_LIMIT_ID, 100);
        commissionRate = Math.min(100, positiveInt(SimConstant.Common.VISIT_COMMISSION_RATE_ID, 3));
        dailyLikeLimit = positiveInt(SimConstant.Common.VISIT_DAILY_LIKE_LIMIT_ID, 10);
        dailyCommentLimit = positiveInt(SimConstant.Common.VISIT_DAILY_COMMENT_LIMIT_ID, 10);
        commentRechargeAmount = positiveDecimal(
                SimConstant.Common.VISIT_COMMENT_RECHARGE_ID, BigDecimal.ONE);
        commentMaxLength = Math.min(MAX_COMMENT_LENGTH,
                positiveInt(SimConstant.Common.VISIT_COMMENT_MAX_LENGTH_ID, MAX_COMMENT_LENGTH));
        recordLimit = MAX_RECORD_LIMIT;
        dailyCommissionLimit = positiveLong(SimConstant.Common.VISIT_DAILY_COMMISSION_LIMIT_ID, 1_000_000);
        trialSessionSeconds = positiveInt(SimConstant.Common.VISIT_TRIAL_SESSION_SECONDS_ID, 1800);
    }

    private int positiveInt(int id, int defaultValue) {
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(id);
        if (cfg == null || cfg.getIntValue() <= 0) {
            log.warn("拜访配置缺失或无效，使用默认值 id={},defaultValue={}", id, defaultValue);
            return defaultValue;
        }
        return cfg.getIntValue();
    }

    private long positiveLong(int id, long defaultValue) {
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(id);
        long value = cfg == null ? 0 : Math.max(cfg.getLongValue(), cfg.getIntValue());
        if (value <= 0) {
            log.warn("拜访配置缺失或无效，使用默认值 id={},defaultValue={}", id, defaultValue);
            return defaultValue;
        }
        return value;
    }

    private BigDecimal positiveDecimal(int id, BigDecimal defaultValue) {
        GlobalConfigCfg cfg = GameDataManager.getGlobalConfigCfg(id);
        try {
            BigDecimal value = cfg == null || cfg.getValue() == null
                    ? BigDecimal.ZERO : new BigDecimal(cfg.getValue().trim());
            if (value.compareTo(BigDecimal.ZERO) > 0) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            //统一走默认值和告警
        }
        log.warn("拜访配置缺失或无效，使用默认值 id={},defaultValue={}", id, defaultValue);
        return defaultValue;
    }

    static boolean isValidComment(String content, int maxLength) {
        if (content == null || content.isBlank() || maxLength <= 0) {
            return false;
        }
        return content.codePointCount(0, content.length()) <= maxLength;
    }

    static long calculateCommission(long win, int ratePercent, long remainingLimit) {
        if (win <= 0 || ratePercent <= 0 || remainingLimit <= 0) {
            return 0;
        }
        long commission = win > Long.MAX_VALUE / ratePercent
                ? Long.MAX_VALUE
                : win * ratePercent / 100;
        return Math.min(commission, remainingLimit);
    }

    public int getLikePopularity() { return likePopularity; }
    public int getCommentPopularity() { return commentPopularity; }
    public int getTrialPopularity() { return trialPopularity; }
    public int getDailyPopularityLimit() { return dailyPopularityLimit; }
    public int getDailyTrialLimit() { return dailyTrialLimit; }
    public int getCommissionRate() { return commissionRate; }
    public int getDailyLikeLimit() { return dailyLikeLimit; }
    public int getDailyCommentLimit() { return dailyCommentLimit; }
    public BigDecimal getCommentRechargeAmount() { return commentRechargeAmount; }
    public int getCommentMaxLength() { return commentMaxLength; }
    public int getRecordLimit() { return recordLimit; }
    public long getDailyCommissionLimit() { return dailyCommissionLimit; }
    public int getTrialSessionSeconds() { return trialSessionSeconds; }
}
