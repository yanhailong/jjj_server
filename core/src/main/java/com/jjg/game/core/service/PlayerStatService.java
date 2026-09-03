package com.jjg.game.core.service;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.condition.numeric.PreparedCondition;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.CountDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 玩家累积统计。统计生命周期绑定玩家，不绑定任务。
 */
@Service
public class PlayerStatService {
    private static final Logger log = LoggerFactory.getLogger(PlayerStatService.class);

    public static final int BUILDING_LEVEL = 12207;
    public static final int SLOT_ITEM = 12251;
    public static final int BIG_SHOW = 12252;
    public static final int JACKPOT = 12253;
    public static final int FREE_MODE = 12254;
    public static final int BUILDING_UPGRADE = 12255;
    public static final int BUILDING_COUNT = 12256;
    public static final int AD_WATCH = 12257;
    public static final int EMPLOYEE_COUNT = 12259;
    public static final int GUEST_RECRUIT = 12260;
    public static final int GUEST_COUNT = 12261;
    public static final int BUSINESS_INCOME = 12262;
    public static final int GAME_UNLOCK = 12263;
    public static final int VISIT = 12264;
    public static final int LOGIN_DAYS = 12265;
    public static final int CASINO_UNLOCK = 12266;
    public static final int CURRENCY_CONSUME = 12267;
    public static final int SCENE_TOTAL_LEVEL = 12268;
    public static final int SKILL_COMBAT_POWER = 12269;
    public static final int GUEST_POOL_DRAW = 12270;
    public static final int EMPLOYEE_POOL_DRAW = 12271;
    public static final int SLOT_BET = 12272;
    public static final int BUILDING_UNLOCK = 12273;
    public static final int SLOT_WIN = 12274;
    public static final int WEALTH_GOD_MODE = 12275;
    public static final int SKILL_TOTAL_LEVEL = 12276;
    public static final int SKILL_LEVEL = 12277;
    public static final int ALLIANCE_DONATE = 12278;
    public static final int VISIT_GIFT = 12279;
    public static final int VISIT_SLOT_SPIN = 12280;
    public static final int DOUXIAN_SETTLEMENT = 12281;
    public static final int SEASON_GEM_CRAFT = 12282;
    public static final int CARD_POOL_DRAW = 12283;

    public static final int GOLD_ITEM_ID = 1990000;
    public static final int DIAMOND_ITEM_ID = 1980000;

    private final CountDao countDao;

    public PlayerStatService(CountDao countDao) {
        this.countDao = countDao;
    }

    public static boolean supports(int conditionId) {
        return conditionId == BUILDING_LEVEL
                || conditionId >= SLOT_ITEM && conditionId <= CARD_POOL_DRAW && conditionId != 12258;
    }

    public static boolean recorded(int conditionId) {
        return switch (conditionId) {
            case SLOT_ITEM, BIG_SHOW, JACKPOT, FREE_MODE, BUILDING_UPGRADE, AD_WATCH,
                    GUEST_RECRUIT, BUSINESS_INCOME, GAME_UNLOCK, VISIT, LOGIN_DAYS,
                    CURRENCY_CONSUME, GUEST_POOL_DRAW, EMPLOYEE_POOL_DRAW, SLOT_BET,
                    SLOT_WIN, WEALTH_GOD_MODE, ALLIANCE_DONATE, VISIT_GIFT, VISIT_SLOT_SPIN,
                    DOUXIAN_SETTLEMENT, SEASON_GEM_CRAFT, CARD_POOL_DRAW -> true;
            default -> false;
        };
    }

    public void recordSlotItems(long playerId, int gameType, Map<Integer, Long> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        items.forEach((itemId, count) -> {
            if (itemId == null || itemId <= 0 || count == null || count <= 0) {
                return;
            }
            incrementPlayerDimension(SLOT_ITEM, playerId, gameType, itemId, count);
            if (gameType != 0) {
                incrementPlayerDimension(SLOT_ITEM, playerId, 0, itemId, count);
            }
        });
    }

    public void recordBigShow(long playerId, int gameType, int bigShowId) {
        if (bigShowId <= 0) {
            return;
        }
        incrementDimensionsPlayer(BIG_SHOW, gameType, bigShowId, playerId, 1);
        if (gameType != 0) {
            incrementDimensionsPlayer(BIG_SHOW, 0, bigShowId, playerId, 1);
        }
    }

    public void recordJackpots(long playerId, int gameType, Map<Integer, Long> jackpotCounts) {
        if (jackpotCounts == null || jackpotCounts.isEmpty()) {
            return;
        }
        jackpotCounts.forEach((jackpotId, count) -> {
            if (jackpotId == null || count == null || count <= 0) {
                return;
            }
            incrementDimensionsPlayer(JACKPOT, gameType, jackpotId, playerId, count);
            if (gameType != 0) {
                incrementDimensionsPlayer(JACKPOT, 0, jackpotId, playerId, count);
            }
        });
    }

    public void recordFreeMode(long playerId, int gameType) {
        incrementDimensionPlayer(FREE_MODE, gameType, playerId, 1);
        if (gameType != 0) {
            incrementDimensionPlayer(FREE_MODE, 0, playerId, 1);
        }
    }

    public void recordBuildingUpgrade(long playerId, int buildingId) {
        incrementPlayerItem(BUILDING_UPGRADE, playerId, buildingId, 1);
        if (buildingId != 0) {
            incrementPlayerItem(BUILDING_UPGRADE, playerId, 0, 1);
        }
    }

    public void recordAdWatch(long playerId) {
        incrementPlayer(AD_WATCH, playerId, 1);
    }

    public void recordGuestRecruit(long playerId, boolean paid, long count) {
        if (count > 0) {
            incrementDimensionPlayer(GUEST_RECRUIT, paid ? 1 : 0, playerId, count);
        }
    }

    public void recordBusinessIncome(long playerId, Map<Integer, Long> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        items.forEach((itemId, count) -> {
            if (itemId == null || count == null || count <= 0) {
                return;
            }
            incrementPlayerItem(BUSINESS_INCOME, playerId, itemId, count);
        });
    }

    public void recordGameUnlock(long playerId, long count) {
        if (count > 0) {
            incrementPlayer(GAME_UNLOCK, playerId, count);
        }
    }

    public void recordGuestPoolDraw(long playerId, long count) {
        if (count > 0) {
            incrementPlayer(GUEST_POOL_DRAW, playerId, count);
        }
    }

    public void recordEmployeePoolDraw(long playerId, long count) {
        if (count > 0) {
            incrementPlayer(EMPLOYEE_POOL_DRAW, playerId, count);
        }
    }

    public void recordSlotBet(long playerId, int gameType) {
        incrementDimensionPlayer(SLOT_BET, gameType, playerId, 1);
        if (gameType != 0) {
            incrementDimensionPlayer(SLOT_BET, 0, playerId, 1);
        }
    }

    public void recordSlotWin(long playerId, int gameType, int itemId, long win) {
        if (win <= 0 || (itemId != GOLD_ITEM_ID && itemId != GameConstant.Item.ID_SEASON_COIN)) {
            return;
        }
        incrementDimensionsPlayer(SLOT_WIN, gameType, itemId, playerId, win);
        if (gameType != 0) {
            incrementDimensionsPlayer(SLOT_WIN, 0, itemId, playerId, win);
        }
    }

    public void recordWealthGodMode(long playerId) {
        incrementPlayer(WEALTH_GOD_MODE, playerId, 1);
    }

    public void recordVisit(long playerId) {
        incrementPlayer(VISIT, playerId, 1);
    }

    public void recordAllianceDonate(long playerId) {
        incrementPlayer(ALLIANCE_DONATE, playerId, 1);
    }

    public void recordVisitGift(long playerId) {
        incrementPlayer(VISIT_GIFT, playerId, 1);
    }

    public void recordVisitSlotSpin(long playerId, int gameType) {
        incrementDimensionPlayer(VISIT_SLOT_SPIN, gameType, playerId, 1);
        if (gameType != 0) {
            incrementDimensionPlayer(VISIT_SLOT_SPIN, 0, playerId, 1);
        }
    }

    public void recordDouXianSettlement(long playerId) {
        incrementPlayer(DOUXIAN_SETTLEMENT, playerId, 1);
    }

    public void recordSeasonGemCraft(long playerId, int quality) {
        incrementPlayer(SEASON_GEM_CRAFT, playerId, 1);
        if (quality > 0) {
            incrementDimensionPlayer(SEASON_GEM_CRAFT, quality, playerId, 1);
        }
    }

    public void recordCardPoolDraw(long playerId, long count) {
        if (count > 0) {
            incrementPlayer(CARD_POOL_DRAW, playerId, count);
        }
    }

    public void recordLoginDay(long playerId) {
        String player = String.valueOf(playerId);
        String dayMarker = feature(LOGIN_DAYS, "login", TimeHelper.getDayNumerical());
        try {
            if (countDao.setIfAbsentHashLong(playerId, dayMarker, player)) {
                incrementPlayer(LOGIN_DAYS, playerId, 1);
            }
        } catch (RuntimeException e) {
            log.error("记录玩家每日登录统计失败 playerId={}", playerId, e);
        }
    }

    public void recordCurrencyConsume(long playerId, int itemId, long count) {
        if (count <= 0 || (itemId != GOLD_ITEM_ID && itemId != DIAMOND_ITEM_ID)) {
            return;
        }
        incrementDimensionPlayer(CURRENCY_CONSUME, itemId, playerId, count);
    }

    public long progress(long playerId, PreparedCondition condition) {
        int conditionId = condition.spec().id();
        return switch (conditionId) {
            case SLOT_ITEM -> getPlayerDimension(SLOT_ITEM, playerId,
                    condition.spec().intParameter(0), condition.spec().intParameter(1));
            case BIG_SHOW -> getDimensionsPlayer(BIG_SHOW, condition.spec().intParameter(0),
                    condition.spec().intParameter(1), playerId);
            case JACKPOT -> getDimensionsPlayer(JACKPOT, condition.spec().intParameter(0),
                    condition.spec().intParameter(1), playerId);
            case FREE_MODE -> getDimensionPlayer(FREE_MODE, condition.spec().intParameter(0), playerId);
            case BUILDING_UPGRADE -> getPlayerItem(BUILDING_UPGRADE, playerId,
                    condition.spec().intParameter(0));
            case AD_WATCH -> getPlayer(AD_WATCH, playerId);
            case GUEST_RECRUIT -> getDimensionPlayer(GUEST_RECRUIT,
                    condition.spec().intParameter(0) > 0 ? 1 : 0, playerId);
            case BUSINESS_INCOME -> getPlayerItemOrSum(BUSINESS_INCOME, playerId,
                    condition.spec().intParameter(0));
            case GAME_UNLOCK -> getPlayer(GAME_UNLOCK, playerId);
            case VISIT -> getPlayer(VISIT, playerId);
            case LOGIN_DAYS -> getPlayer(LOGIN_DAYS, playerId);
            case CURRENCY_CONSUME -> getDimensionPlayer(CURRENCY_CONSUME,
                    condition.spec().intParameter(0), playerId);
            case GUEST_POOL_DRAW -> getPlayer(GUEST_POOL_DRAW, playerId);
            case EMPLOYEE_POOL_DRAW -> getPlayer(EMPLOYEE_POOL_DRAW, playerId);
            case SLOT_BET -> getDimensionPlayer(SLOT_BET, condition.spec().intParameter(0), playerId);
            case SLOT_WIN -> getDimensionsPlayer(SLOT_WIN, condition.spec().intParameter(0),
                    condition.spec().intParameter(1), playerId);
            case WEALTH_GOD_MODE -> getPlayer(WEALTH_GOD_MODE, playerId);
            case ALLIANCE_DONATE, VISIT_GIFT, DOUXIAN_SETTLEMENT, CARD_POOL_DRAW ->
                    getPlayer(conditionId, playerId);
            case VISIT_SLOT_SPIN -> getDimensionPlayer(VISIT_SLOT_SPIN,
                    condition.spec().intParameter(0), playerId);
            case SEASON_GEM_CRAFT -> condition.spec().intParameter(0) == 0
                    ? getPlayer(SEASON_GEM_CRAFT, playerId)
                    : getDimensionPlayer(SEASON_GEM_CRAFT, condition.spec().intParameter(0), playerId);
            default -> 0;
        };
    }

    private void incrementPlayer(int statId, long playerId, long count) {
        increment(feature(statId), playerId, String.valueOf(playerId), count);
    }

    private void incrementDimensionPlayer(int statId, int dimension, long playerId, long count) {
        increment(feature(statId, dimension), playerId, String.valueOf(playerId), count);
    }

    private void incrementDimensionsPlayer(int statId, int firstDimension, int secondDimension,
                                           long playerId, long count) {
        increment(feature(statId, firstDimension, secondDimension), playerId,
                String.valueOf(playerId), count);
    }

    private void incrementPlayerItem(int statId, long playerId, int itemId, long count) {
        increment(feature(statId, playerId), playerId, String.valueOf(itemId), count);
    }

    private void incrementPlayerDimension(int statId, long playerId, int dimension,
                                          int itemId, long count) {
        increment(feature(statId, playerId, dimension), playerId, String.valueOf(itemId), count);
    }

    private void increment(String featureId, long playerId, String field, long count) {
        try {
            countDao.incrHashLong(playerId, featureId, field, count);
        } catch (RuntimeException e) {
            log.error("累加玩家统计失败 playerId={},featureId={},field={},count={}",
                    playerId, featureId, field, count, e);
        }
    }

    private long getPlayer(int statId, long playerId) {
        return get(feature(statId), String.valueOf(playerId));
    }

    private long getDimensionPlayer(int statId, int dimension, long playerId) {
        return get(feature(statId, dimension), String.valueOf(playerId));
    }

    private long getDimensionsPlayer(int statId, int firstDimension, int secondDimension,
                                     long playerId) {
        return get(feature(statId, firstDimension, secondDimension), String.valueOf(playerId));
    }

    private long getPlayerItem(int statId, long playerId, int itemId) {
        return get(feature(statId, playerId), String.valueOf(itemId));
    }

    private long getPlayerItemOrSum(int statId, long playerId, int itemId) {
        return itemId == 0 ? sum(feature(statId, playerId)) : getPlayerItem(statId, playerId, itemId);
    }

    private long getPlayerDimension(int statId, long playerId, int dimension, int itemId) {
        return get(feature(statId, playerId, dimension), String.valueOf(itemId));
    }

    private long get(String featureId, String field) {
        try {
            return countDao.getCountHashLong(featureId, field);
        } catch (RuntimeException e) {
            log.error("读取玩家统计失败 featureId={},field={}", featureId, field, e);
            return 0;
        }
    }

    private long sum(String featureId) {
        try {
            return countDao.sumCountHashLong(featureId);
        } catch (RuntimeException e) {
            log.error("汇总玩家统计失败 featureId={}", featureId, e);
            return 0;
        }
    }

    private static String feature(Object... parts) {
        StringBuilder key = new StringBuilder();
        for (Object part : parts) {
            if (!key.isEmpty()) {
                key.append(':');
            }
            key.append(part);
        }
        return CountDao.CountType.PLAYER_STAT.getParam().formatted(key);
    }
}
