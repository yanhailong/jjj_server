package com.jjg.game.hall.vip.data;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.data.Player;
import com.jjg.game.sampledata.bean.ViplevelCfg;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Function;

/**
 * @author lm
 * @date 2025/8/28 10:43
 */
public enum VipGift {
    WEEKS(1, ViplevelCfg::getWeeklyRewards),
    BIRTHDAY(2, ViplevelCfg::getBirthdayReward),
    PROMOTION(3, ViplevelCfg::getLevelRewards),
    YEAR(4, ViplevelCfg::getAnnualRewards),
    ;
    private final int type;
    private final Function<ViplevelCfg, Map<Integer, Long>> reward;

    VipGift(int type, Function<ViplevelCfg, Map<Integer, Long>> reward) {
        this.type = type;
        this.reward = reward;
    }

    public int getType() {
        return type;
    }

    public Function<ViplevelCfg, Map<Integer, Long>> getReward() {
        return reward;
    }

    public boolean isCanClaim(Player player, Vip vip, long timeMillis) {
        long lastClaim = vip.getGiftGetTime().getOrDefault(type, 0L);
        return switch (this) {
            case WEEKS -> !TimeHelper.inSameWeek(lastClaim, timeMillis);
            case YEAR ->
                    !TimeHelper.isLastDayOfYear(lastClaim) && TimeHelper.isLastDayOfYear(System.currentTimeMillis());
            case BIRTHDAY -> {
                long epochMilli = LocalDateTime.ofInstant(Instant.ofEpochMilli(player.getCreateTime()), ZoneId.systemDefault())
                        .plusYears(1).toLocalDate().atStartOfDay()
                        .atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
                yield !TimeHelper.inSameDay(lastClaim, epochMilli) && TimeHelper.inSameDay(timeMillis, epochMilli);
            }
            case PROMOTION -> {
                if (CollectionUtil.isEmpty(vip.getLvGiftGetTime())) {
                    yield true;
                }
                for (int i = 1; i < player.getVipLevel(); i++) {
                    if (!vip.getLvGiftGetTime().containsKey(i)) {
                        yield true;
                    }
                }
                yield false;
            }
        };
    }

    public long getNextClaimNeed(Player player, boolean time) {
        return switch (this) {
            case WEEKS -> {
                if (!time) {
                    yield 0;
                }
                yield TimeHelper.getNextWeekdayEnd(DayOfWeek.MONDAY);
            }
            case YEAR -> {
                if (!time) {
                    yield 0;
                }
                yield TimeHelper.getYearDayEnd();
            }
            case BIRTHDAY -> {
                if (!time) {
                    yield 0;
                }
                yield LocalDateTime.ofInstant(Instant.ofEpochSecond(player.getCreateTime()), ZoneId.systemDefault())
                        .plusYears(1).toLocalDate().atStartOfDay()
                        .atZone(ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
            }
            case PROMOTION -> {
                if (time) {
                    yield 0;
                }
                ViplevelCfg viplevelCfg = VipCfgCache.getVipLevelCfg(player.getVipLevel());
                yield (viplevelCfg.getViplevelUpExp() - player.getVipExp()) * viplevelCfg.getRecharge() / 10000;
            }
        };
    }

}
