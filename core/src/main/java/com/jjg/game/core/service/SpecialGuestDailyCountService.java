package com.jjg.game.core.service;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.dao.CountDao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 记录特殊游客配置的玩家每日使用次数。
 *
 * <p>广告观看次数与付费购买次数使用独立 Redis 计数，避免多节点并发时仅修改玩家内存数据。
 * 广告次数按玩家当天全局累计，付费次数按配置累计；过期时间保留两天，既覆盖跨天结算窗口，
 * 也不会长期占用存储。</p>
 */
@Service
public class SpecialGuestDailyCountService {
    private static final String AD_COUNT = "simSpecialGuestAd:%d";
    private static final String PAID_COUNT = "simSpecialGuestPaid:%d:%d";
    private static final long EXPIRE_SECONDS = TimeHelper.DAY_SECOND * 2L;

    private final CountDao countDao;

    public SpecialGuestDailyCountService(CountDao countDao) {
        this.countDao = countDao;
    }

    /** 获取玩家当天已观看的特殊游客广告总次数。 */
    public int getAdCount(long playerId) {
        return countDao.getCount(AD_COUNT.formatted(TimeHelper.getDayNumerical()),
                String.valueOf(playerId)).intValue();
    }

    /** 增加一次特殊游客广告观看次数并返回增加后的当天总次数。 */
    public int addAdCount(long playerId) {
        return countDao.incrementWithoutExpireRefresh(
                AD_COUNT.formatted(TimeHelper.getDayNumerical()), String.valueOf(playerId),
                BigDecimal.ONE, EXPIRE_SECONDS).intValue();
    }

    /** 获取玩家当天对指定付费游客配置的已购买次数。 */
    public int getPaidCount(long playerId, int cfgId) {
        return getCount(PAID_COUNT, playerId, cfgId);
    }

    /** 增加一次购买次数并返回增加后的当天次数。 */
    public int addPaidCount(long playerId, int cfgId) {
        return addCount(PAID_COUNT, playerId, cfgId);
    }

    private int getCount(String feature, long playerId, int cfgId) {
        return countDao.getCount(feature.formatted(TimeHelper.getDayNumerical(), cfgId),
                String.valueOf(playerId)).intValue();
    }

    private int addCount(String feature, long playerId, int cfgId) {
        return countDao.incrementWithoutExpireRefresh(
                feature.formatted(TimeHelper.getDayNumerical(), cfgId), String.valueOf(playerId),
                BigDecimal.ONE, EXPIRE_SECONDS).intValue();
    }
}
