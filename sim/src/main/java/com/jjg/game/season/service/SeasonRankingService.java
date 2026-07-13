package com.jjg.game.season.service;

import com.jjg.game.season.dao.SeasonPlayerDao;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.data.SeasonRankEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按赛季隔离键查询总榜。
 * <p>
 * 榜单与名次都是展示数据, 直查 Mongo 的成本随同赛季人数线性增长
 * (top 榜是索引排序取前 N, 名次是 count 扫索引区间), 高在线下会把数据库打满;
 * 这里按 seasonKey 做短 TTL 榜单缓存, 名次走玩家数据上的线程本地缓存
 * (TTL 内且自身币值未变时复用)。结算发奖必须用 {@link #freshRankOf} 取实时名次。
 */
@Service
public class SeasonRankingService {
    private static final long RANKING_CACHE_TTL_MILLIS = 30_000L;
    private static final long RANK_OF_TTL_MILLIS = 30_000L;
    /**
     * 超过该键数时顺带清理过期缓存; 活跃 seasonKey 数量有限, 正常不会触发。
     */
    private static final int CACHE_KEY_EVICT_THRESHOLD = 64;

    private final SeasonPlayerDao seasonPlayerDao;
    private final ConcurrentHashMap<String, CachedRanking> rankingCache = new ConcurrentHashMap<>();

    public SeasonRankingService(SeasonPlayerDao seasonPlayerDao) {
        this.seasonPlayerDao = seasonPlayerDao;
    }

    /**
     * 返回的列表为共享缓存, 只读使用。
     */
    public List<SeasonRankEntry> ranking(String seasonKey, int limit) {
        long now = System.currentTimeMillis();
        CachedRanking cached = rankingCache.get(seasonKey);
        if (cached == null || now - cached.cachedAt >= RANKING_CACHE_TTL_MILLIS || cached.limit < limit) {
            cached = new CachedRanking(now, limit, loadRanking(seasonKey, limit));
            rankingCache.put(seasonKey, cached);
            evictStale(now);
        }
        List<SeasonRankEntry> entries = cached.entries;
        return entries.size() <= limit ? entries : entries.subList(0, limit);
    }

    /**
     * 展示用名次: 玩家线程本地缓存, TTL 内且自身币值未变直接复用, 避免每个 info 请求都触发 count 扫描。
     */
    public int rankOf(SeasonPlayerData data) {
        long now = System.currentTimeMillis();
        if (data.getRankCacheTime() > 0 && now - data.getRankCacheTime() < RANK_OF_TTL_MILLIS
                && data.getRankCacheCoin() == data.getSeasonCoin()
                && data.getRankCacheEarned() == data.getTotalEarnedCoin()) {
            return data.getRankCacheValue();
        }
        int rank = freshRankOf(data);
        data.setRankCacheTime(now);
        data.setRankCacheCoin(data.getSeasonCoin());
        data.setRankCacheEarned(data.getTotalEarnedCoin());
        data.setRankCacheValue(rank);
        return rank;
    }

    /**
     * 实时名次: 结算发奖等影响收益的路径使用, 不走缓存。
     */
    public int freshRankOf(SeasonPlayerData data) {
        return Math.toIntExact(seasonPlayerDao.findRank(data.getSeasonKey(), data.getPlayerId(),
                data.getSeasonCoin(), data.getTotalEarnedCoin()));
    }

    private List<SeasonRankEntry> loadRanking(String seasonKey, int limit) {
        List<SeasonPlayerData> players = seasonPlayerDao.findRanking(seasonKey, limit);
        List<SeasonRankEntry> result = new ArrayList<>(players.size());
        for (int index = 0; index < players.size(); index++) {
            SeasonPlayerData player = players.get(index);
            SeasonRankEntry entry = new SeasonRankEntry();
            entry.setRank(index + 1);
            entry.setPlayerId(player.getPlayerId());
            entry.setSeasonCoin(player.getSeasonCoin());
            entry.setTotalEarnedCoin(player.getTotalEarnedCoin());
            entry.setTierId(player.getTierId());
            result.add(entry);
        }
        return result;
    }

    private void evictStale(long now) {
        if (rankingCache.size() <= CACHE_KEY_EVICT_THRESHOLD) {
            return;
        }
        rankingCache.values().removeIf(cached -> now - cached.cachedAt >= RANKING_CACHE_TTL_MILLIS);
    }

    private record CachedRanking(long cachedAt, int limit, List<SeasonRankEntry> entries) {
    }
}
