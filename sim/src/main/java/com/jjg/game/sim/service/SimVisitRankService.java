package com.jjg.game.sim.service;

import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.RankEntry;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.RankService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PopularityRankingCfg;
import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.pb.res.ResVisitRank;
import com.jjg.game.sim.pb.struct.VisitRankInfo;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 拜访人气自然月赛季榜。
 *
 * @author 11
 * @date 2026/6/30
 */
@Service
public class SimVisitRankService {
    private static final Logger log = LoggerFactory.getLogger(SimVisitRankService.class);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int SHOW_COUNT = 100;
    private static final long TOP_CACHE_MILLIS = 10_000;

    //top100 节点内短缓存 (对全服玩家内容相同, 拜访界面+主界面两处高频入口)
    private volatile CachedTop cachedTop;

    private final RankService rankService;
    private final CorePlayerService corePlayerService;
    private final SimCasinoDao simCasinoDao;
    private final MailService mailService;
    private final RedissonClient redissonClient;

    public SimVisitRankService(RankService rankService,
                               CorePlayerService corePlayerService,
                               SimCasinoDao simCasinoDao,
                               MailService mailService,
                               RedissonClient redissonClient) {
        this.rankService = rankService;
        this.corePlayerService = corePlayerService;
        this.simCasinoDao = simCasinoDao;
        this.mailService = mailService;
        this.redissonClient = redissonClient;
    }

    public long addPopularity(long playerId, int points) {
        if (playerId <= 0 || points <= 0) {
            return 0;
        }
        return rankService.addPoints(rankKey(LocalDate.now()), playerId, points);
    }

    public ResVisitRank buildRank(long playerId) {
        ResVisitRank res = new ResVisitRank(Code.SUCCESS);
        LocalDate today = LocalDate.now();
        String key = rankKey(today);
        res.ranks = topRanks(key);
        RankEntry myEntry = rankService.getRank(key, playerId);
        if (myEntry == null) {
            myEntry = new RankEntry(playerId, 0, -1);
        }
        Map<Long, Player> players = corePlayerService.multiGetPlayerMap(List.of(playerId));
        Map<Long, Integer> casinoLevels = simCasinoDao.findMaxCasinoLevel(List.of(playerId));
        res.my = toInfo(myEntry, players, casinoLevels);
        res.seasonEndTime = today.withDayOfMonth(1).plusMonths(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return res;
    }

    /**
     * top100 短缓存读取: 命中直接复用组装结果, 未命中在锁内重建, 防止并发请求打穿
     * 100 玩家的 Player/赌场等级批量查询; 自己的名次与积分仍每次实时读取。
     */
    private List<VisitRankInfo> topRanks(String key) {
        long now = System.currentTimeMillis();
        CachedTop cached = cachedTop;
        if (cached != null && cached.key().equals(key) && now < cached.expireAt()) {
            return cached.ranks();
        }
        synchronized (this) {
            cached = cachedTop;
            if (cached != null && cached.key().equals(key) && now < cached.expireAt()) {
                return cached.ranks();
            }
            List<RankEntry> entries = rankService.topN(key, SHOW_COUNT);
            List<Long> ids = entries.stream().map(RankEntry::getPlayerId).toList();
            Map<Long, Player> players = corePlayerService.multiGetPlayerMap(ids);
            Map<Long, Integer> casinoLevels = simCasinoDao.findMaxCasinoLevel(ids);
            List<VisitRankInfo> ranks = new ArrayList<>(entries.size());
            for (RankEntry entry : entries) {
                VisitRankInfo info = toInfo(entry, players, casinoLevels);
                if (info != null) {
                    ranks.add(info);
                }
            }
            cachedTop = new CachedTop(key, List.copyOf(ranks), now + TOP_CACHE_MILLIS);
            return cachedTop.ranks();
        }
    }

    private record CachedTop(String key, List<VisitRankInfo> ranks, long expireAt) {
    }

    private VisitRankInfo toInfo(RankEntry entry, Map<Long, Player> players,
                                 Map<Long, Integer> casinoLevels) {
        Player player = players.get(entry.getPlayerId());
        if (player == null) {
            return null;
        }
        VisitRankInfo info = new VisitRankInfo();
        info.rank = (int) entry.getRank();
        info.playerId = entry.getPlayerId();
        info.playerName = player.getNickName();
        info.headImgId = player.getHeadImgId();
        info.headFrameId = player.getHeadFrameId();
        info.casinoLevel = casinoLevels.getOrDefault(entry.getPlayerId(), 0);
        info.popularity = entry.getPoints();
        Map<Integer, Long> rewards = rewardForRank(info.rank);
        info.rewards = rewards.isEmpty() ? Collections.emptyList() : ItemUtils.buildItemInfo(rewards);
        return info;
    }

    /**
     * leader 节点调用，按结算标记和锁保证同一赛季只发一次。
     */
    public void settlePreviousSeason() {
        LocalDate today = LocalDate.now();
        String oldKey = previousRankKey(today);
        String settledKey = oldKey + ":settled";
        RBucket<Boolean> marker = redissonClient.getBucket(settledKey);
        if (Boolean.TRUE.equals(marker.get())) {
            return;
        }
        RLock lock = redissonClient.getLock(oldKey + ":settle-lock");
        if (!lock.tryLock()) {
            return;
        }
        try {
            if (Boolean.TRUE.equals(marker.get())) {
                return;
            }
            List<RankEntry> entries = rankService.topN(oldKey, SHOW_COUNT);
            for (RankEntry entry : entries) {
                Map<Integer, Long> rewards = rewardForRank((int) entry.getRank());
                if (rewards.isEmpty()) {
                    continue;
                }
                List<Item> items = new ArrayList<>(rewards.size());
                rewards.forEach((id, count) -> items.add(new Item(id, count)));
                //发奖循环可能中途异常后整体重试, 按赛季+玩家业务键幂等, 避免已发玩家重复领奖
                mailService.addMailIfAbsent(entry.getPlayerId(), "人气榜赛季奖励",
                        "上赛季人气榜第" + entry.getRank() + "名奖励", items,
                        AddType.SIM_VISIT_RANK_REWARD, oldKey + ":reward:" + entry.getPlayerId());
            }
            marker.set(true, Duration.ofDays(90));
            rankService.reset(oldKey);
            log.info("拜访人气榜赛季结算完成 key={},players={}", oldKey, entries.size());
        } catch (Exception e) {
            log.error("拜访人气榜赛季结算失败 key={}", oldKey, e);
        } finally {
            lock.unlock();
        }
    }

    static String rankKey(LocalDate day) {
        return "sim:visit:rank:" + MONTH.format(day);
    }

    static String previousRankKey(LocalDate day) {
        return rankKey(day.minusMonths(1));
    }

    /**
     * PopularityRanking 表 ranking 列: 单值为精确名次, 两值为名次区间 [from, to]。
     */
    static Map<Integer, Long> rewardForRank(int rank) {
        if (rank <= 0) {
            return Collections.emptyMap();
        }
        for (PopularityRankingCfg cfg : GameDataManager.getPopularityRankingCfgList()) {
            List<Integer> ranking = cfg.getRanking();
            if (ranking == null || ranking.isEmpty()) {
                continue;
            }
            int from = ranking.get(0);
            int to = ranking.size() > 1 ? ranking.get(1) : from;
            if (rank >= from && rank <= to) {
                Map<Integer, Long> rewards = cfg.getGetItem();
                return rewards == null ? Collections.emptyMap() : rewards;
            }
        }
        return Collections.emptyMap();
    }
}
