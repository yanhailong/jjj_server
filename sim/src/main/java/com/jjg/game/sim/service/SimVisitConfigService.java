package com.jjg.game.sim.service;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.data.SimVisitGiftConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 拜访系统全局配置缓存，初始化和 global.xlsx 热更共用同一加载入口。
 *
 * @author 11
 * @date 2026/6/30
 */
@Service
public class SimVisitConfigService implements ConfigExcelChangeListener {
    private static final Logger log = LoggerFactory.getLogger(SimVisitConfigService.class);
    private static final List<SimVisitGiftConfig> DEFAULT_GIFTS =
            List.of(new SimVisitGiftConfig(1, 10, 5));
    private static final int MAX_COMMENT_LENGTH = 30;
    private static final int MAX_RECORD_LIMIT = 100;
    private static final int MAX_GIFT_COUNT = 100;

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
    private volatile List<SimVisitGiftConfig> gifts = DEFAULT_GIFTS;
    private volatile long dailyCommissionLimit = 1_000_000;
    private volatile int trialSessionSeconds = 1800;
    private volatile String rankRewardConfig = "";

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
        recordLimit = Math.min(MAX_RECORD_LIMIT,
                positiveInt(SimConstant.Common.VISIT_RECORD_LIMIT_ID, MAX_RECORD_LIMIT));
        dailyCommissionLimit = positiveLong(SimConstant.Common.VISIT_DAILY_COMMISSION_LIMIT_ID, 1_000_000);
        trialSessionSeconds = positiveInt(SimConstant.Common.VISIT_TRIAL_SESSION_SECONDS_ID, 1800);

        GlobalConfigCfg giftCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.VISIT_GIFT_LIST_ID);
        List<SimVisitGiftConfig> parsed = parseGiftConfigs(giftCfg == null ? null : giftCfg.getValue());
        gifts = parsed.isEmpty() ? DEFAULT_GIFTS : parsed;
        GlobalConfigCfg rewardCfg = GameDataManager.getGlobalConfigCfg(SimConstant.Common.VISIT_RANK_REWARD_ID);
        rankRewardConfig = rewardCfg == null || rewardCfg.getValue() == null ? "" : rewardCfg.getValue().trim();
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

    static List<SimVisitGiftConfig> parseGiftConfigs(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        Map<Integer, SimVisitGiftConfig> result = new LinkedHashMap<>();
        for (String segment : value.split("\\|")) {
            String[] fields = segment.split("_");
            if (fields.length != 3) {
                continue;
            }
            try {
                int id = Integer.parseInt(fields[0].trim());
                long cost = Long.parseLong(fields[1].trim());
                int popularity = Integer.parseInt(fields[2].trim());
                if (id > 0 && cost > 0 && popularity > 0) {
                    result.putIfAbsent(id, new SimVisitGiftConfig(id, cost, popularity));
                    if (result.size() >= MAX_GIFT_COUNT) {
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {
                //单条配置错误不影响其它礼物
            }
        }
        return List.copyOf(result.values());
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

    public SimVisitGiftConfig findGift(int giftId) {
        for (SimVisitGiftConfig gift : gifts) {
            if (gift.id() == giftId) {
                return gift;
            }
        }
        return null;
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
    public List<SimVisitGiftConfig> getGifts() { return gifts; }
    public long getDailyCommissionLimit() { return dailyCommissionLimit; }
    public int getTrialSessionSeconds() { return trialSessionSeconds; }
    public String getRankRewardConfig() { return rankRewardConfig; }
}
