package com.jjg.game.hall.vip.data;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.data.Player;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.ViplevelCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    CUSTOMIZED(4, ViplevelCfg::getAnnualRewards),
    ;
    private static final Logger log = LoggerFactory.getLogger(VipGift.class);
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
            case CUSTOMIZED -> !TimeHelper.inSameDay(lastClaim, timeMillis) && canClaimCustomized();
        };
    }

    /**
     * 是否能领取自定义礼包
     * @return 是否能领取
     */
    private boolean canClaimCustomized() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(56);
        if (globalConfigCfg == null) {
            return false;
        }
        try {
            String globalConfigCfgValue = globalConfigCfg.getValue();
            if (StringUtils.isEmpty(globalConfigCfgValue)) {
                return false;
            }
            String[] timeCfgArr = StringUtils.split(globalConfigCfgValue, "_");
            LocalDateTime now = LocalDateTime.now();
            int nowTimeInt = now.getMonthValue() * 100 + now.getDayOfMonth();
            for (String timeCfg : timeCfgArr) {
                //时间长度必须为4
                if (timeCfg.length() != 4) {
                    return false;
                }
                if (nowTimeInt == Integer.parseInt(timeCfg)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("canClaimCustomized error: {}", globalConfigCfg.getValue(), e);
        }
        return false;
    }

    /**
     * 是否能领取自定义礼包
     * @return 是否能领取
     */
    private long nextClaimCustomizedTime() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(56);
        if (globalConfigCfg == null) {
            return 0;
        }
        try {
            String globalConfigCfgValue = globalConfigCfg.getValue();
            if (StringUtils.isEmpty(globalConfigCfgValue)) {
                return 0;
            }
            String[] timeCfgArr = StringUtils.split(globalConfigCfgValue, "_");
            LocalDateTime now = LocalDateTime.now();
            int nowTimeInt = now.getMonthValue() * 100 + now.getDayOfMonth();
            for (String timeCfg : timeCfgArr) {
                //时间长度必须为4
                if (timeCfg.length() != 4) {
                    return 0;
                }
                int timeCfgInt = Integer.parseInt(timeCfg);
                if (nowTimeInt < timeCfgInt) {
                    //解析出来
                    return TimeHelper.getTimestamp(LocalDateTime.of(now.getYear(), timeCfgInt / 100, timeCfgInt % 100, 0, 0));
                }
            }
        } catch (Exception e) {
            log.error("nextClaimCustomizedTime error: {}", globalConfigCfg.getValue(), e);
        }
        return 0;
    }

    public long getNextClaimNeed(Player player, boolean time) {
        return switch (this) {
            case WEEKS -> {
                if (!time) {
                    yield 0;
                }
                yield TimeHelper.getNextWeekdayEnd(DayOfWeek.MONDAY);
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
            case CUSTOMIZED -> {
                if (time) {
                    yield nextClaimCustomizedTime();
                }
                yield 0;
            }
        };
    }

}
