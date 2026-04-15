package com.jjg.game.poker.game.tosouth.cardlib;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.poker.game.common.cardlib.AbstractCardLibDao;
import org.springframework.stereotype.Component;

/**
 * 南方前进牌库 Redis DAO
 * 通用能力（双库切换、批量读写、水池余额、锁）全部由 {@link AbstractCardLibDao} 提供。
 * 本类只保留南方前进特有的 Redis key（玩家连胜/盈亏）。
 *
 * Redis key 命名规则与 Slots 对齐：PokerResultLib{N}:{gameType}:{sectionKey}
 */
@Component
public class ToSouthCardLibDao extends AbstractCardLibDao<ToSouthCardLib> {

    private static final int GAME_TYPE = CoreConst.GameType.TO_SOUTH; // 300400

    /** 玩家连赢/连输记录 HASH: playerId → streak */
    private static final String PLAYER_STREAK_KEY = "PokerResultLibStreak:" + GAME_TYPE;

    /** 玩家总盈亏记录 HASH: playerId → totalProfit */
    private static final String PLAYER_PROFIT_KEY = "PokerResultLibProfit:" + GAME_TYPE;

    public ToSouthCardLibDao() {
        super(ToSouthCardLib.class,
                "PokerResultLibCurrent:" + GAME_TYPE,        // currentLibKey
                "PokerResultLib1:" + GAME_TYPE + ":",         // lib1Prefix
                "PokerResultLib2:" + GAME_TYPE + ":",         // lib2Prefix
                "PokerResultLibGenLock:" + GAME_TYPE,         // genLockKey
                "PokerResultLibLastGenTime:" + GAME_TYPE);    // lastGenTimeKey
    }

    // ==================== 玩家统计数据（持久化到 Redis，跨房间保留） ====================

    /**
     * 获取玩家连赢/连输值
     *
     * @param playerId 玩家 ID
     * @return 正=连赢次数, 负=连输次数, 0=无记录
     */
    public int getPlayerWinStreak(long playerId) {
        Object val = redisTemplate.opsForHash().get(PLAYER_STREAK_KEY, String.valueOf(playerId));
        if (val == null) return 0;
        return Integer.parseInt(val.toString());
    }

    /**
     * 设置玩家连赢/连输值
     */
    public void setPlayerWinStreak(long playerId, int streak) {
        redisTemplate.opsForHash().put(PLAYER_STREAK_KEY, String.valueOf(playerId), String.valueOf(streak));
    }

    /**
     * 获取玩家总盈亏
     *
     * @param playerId 玩家 ID
     * @return 总盈亏（正=盈利, 负=亏损）
     */
    public long getPlayerTotalProfit(long playerId) {
        Object val = redisTemplate.opsForHash().get(PLAYER_PROFIT_KEY, String.valueOf(playerId));
        if (val == null) return 0;
        return Long.parseLong(val.toString());
    }

    /**
     * 累加玩家总盈亏
     *
     * @param playerId 玩家 ID
     * @param delta    本局盈亏变化
     */
    public void addPlayerTotalProfit(long playerId, long delta) {
        redisTemplate.opsForHash().increment(PLAYER_PROFIT_KEY, String.valueOf(playerId), delta);
    }
}
